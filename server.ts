import express from "express";
import path from "path";
import fs from "fs";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI, Type } from "@google/genai";
import dotenv from "dotenv";

dotenv.config();

const app = express();
const PORT = 3000;

// Set up json parser with larger limit since we receive base64 PDFs
app.use(express.json({ limit: "50mb" }));

const DB_PATH = path.join(process.cwd(), "db.json");

// Helper: Initialize/Read local JSON DB
function getDatabase() {
  try {
    if (!fs.existsSync(DB_PATH)) {
      const initialDb = { entries: [], logs: [] };
      fs.writeFileSync(DB_PATH, JSON.stringify(initialDb, null, 2), "utf8");
      return initialDb;
    }
    const raw = fs.readFileSync(DB_PATH, "utf8");
    return JSON.parse(raw);
  } catch (e) {
    console.error("Failed to read database, resetting:", e);
    return { entries: [], logs: [] };
  }
}

// Helper: Save local JSON DB
function saveDatabase(data: any) {
  try {
    fs.writeFileSync(DB_PATH, JSON.stringify(data, null, 2), "utf8");
  } catch (e) {
    console.error("Failed to save database:", e);
  }
}

// Helper: Add log entry
function logAction(action: string, details: string) {
  const db = getDatabase();
  db.logs.unshift({
    timestamp: new Date().toISOString(),
    action,
    details,
  });
  // Keep logs at a reasonable limit (e.g. 500)
  if (db.logs.length > 500) {
    db.logs = db.logs.slice(0, 500);
  }
  saveDatabase(db);
}

// Helper: Fetch vehicle drivetrain & epb details from NHTSA with a strict 3.5s timeout
async function fetchNhtsaData(vin: string) {
  try {
    const cleanVin = vin.trim().toUpperCase();
    if (!cleanVin || cleanVin.length < 10) {
      return { drivetrain: "Unknown", epb: "No" };
    }
    const url = `https://vpic.nhtsa.dot.gov/api/vehicles/DecodeVinValues/${cleanVin}?format=json`;
    
    // Add a strict timeout of 3.5s so slow NHTSA services don't hang the app
    const controller = new AbortController();
    const timeoutId = setTimeout(() => controller.abort(), 3500);

    const res = await fetch(url, { signal: controller.signal });
    clearTimeout(timeoutId);

    if (!res.ok) {
      return { drivetrain: "Unknown", epb: "No" };
    }
    const data = await res.json();
    const result = data?.Results?.[0];
    if (!result) {
      return { drivetrain: "Unknown", epb: "No" };
    }

    // DriveType parsing
    let driveTypeRaw = (result.DriveType || result["Drive Type"] || "").toLowerCase();
    let drivetrain = "Rear Wheel"; // default fallback or Rear Wheel
    if (driveTypeRaw.includes("all") || driveTypeRaw.includes("awd") || driveTypeRaw.includes("4wd") || driveTypeRaw.includes("4x4") || driveTypeRaw.includes("four")) {
      drivetrain = "All wheel drive,";
    } else if (driveTypeRaw.includes("front") || driveTypeRaw.includes("fwd")) {
      drivetrain = "Front wheel drive";
    } else if (driveTypeRaw.includes("rear") || driveTypeRaw.includes("rwd") || driveTypeRaw.includes("4x2")) {
      drivetrain = "Rear Wheel";
    } else if (driveTypeRaw) {
      // capitalize nicely
      drivetrain = driveTypeRaw.split(' ').map((w: string) => w.charAt(0).toUpperCase() + w.slice(1)).join(' ');
    }

    // EPB parsing
    let epbRaw = (result.ParkBrakeType || result["Park Brake Type"] || "").toLowerCase();
    let epb = "No";
    if (epbRaw.includes("electric") || epbRaw.includes("electronic") || epbRaw.includes("epb") || epbRaw.includes("yes")) {
      epb = "Yes";
    }

    return { drivetrain, epb };
  } catch (error) {
    console.error(`NHTSA fetch error for VIN ${vin}:`, error);
    return { drivetrain: "Unknown", epb: "No" };
  }
}

// API: Get Local Database (entries & logs)
app.get("/api/database", (req, res) => {
  const db = getDatabase();
  res.json(db);
});

// API: Process PDF via OCR (Gemini API)
app.post("/api/ocr", async (req, res) => {
  try {
    const { files } = req.body;
    if (!files || !Array.isArray(files) || files.length === 0) {
       res.status(400).json({ error: "No files provided in request body." });
       return;
    }

    const apiKey = process.env.GEMINI_API_KEY;
    if (!apiKey) {
       res.status(500).json({ error: "GEMINI_API_KEY is not configured on the server secrets." });
       return;
    }

    const ai = new GoogleGenAI({
      apiKey,
      httpOptions: {
        headers: {
          "User-Agent": "aistudio-build",
        },
      },
    });

    // Prompt details:
    // Extract:
    // (id, date_hauled, vehicle_details, vin, origin, destination, for_client, price) for Jenkins_Paysheet
    // And (manifest, vehicle_details, vin, origin, destination, hand_written) for Daily Loads
    // Note: If hand written notes have any number with a $ that becomes the price
    const prompt = `Perform high-quality OCR scanning on the attached transport/haulage manifest PDF.
Note that the PDF may contain multiple pages, with each page or section representing a distinct haulage manifest/load.
Analyze all pages of the document, and return an array of all distinct manifests/loads found across all pages in the 'records' field.
Ensure you inspect all written/printed content and hand-written notes carefully.

For each record, pay special attention to the following fields:
1. Manifest/Load ID (id)
2. Date Hauled (date_hauled) - if not explicitly found, leave empty.
3. Vehicle Details (vehicle_details) - like Year, Make, Model of the vehicle hauled.
4. VIN (vin) - 17-character vehicle identification number.
5. Origin (origin) - starting city/state or depot.
6. Destination (destination) - delivery city/state or yard.
7. For / Client (for_client) - the customer/client name the haul is performed for.
8. Hand Written Notes (hand_written) - any handwritten notes, comments, price quotes, or instructions on the sheet.
9. Price (price) - Look closely at all prices. CRITICAL RULE: If the hand written notes or anywhere on the page has any number with a $ (e.g., "$150", "pay $250", "$320.00"), extract this exact number as the price. Otherwise, extract the printed price. If no price is listed at all, return 0.

Return the result as a strictly formatted JSON object matching the requested schema.`;

    // Process all files in parallel
    const results = await Promise.all(
      files.map(async (file) => {
        const base64Data = file.data.replace(/^data:application\/pdf;base64,/, "");

        const response = await ai.models.generateContent({
          model: "gemini-3.5-flash",
          contents: [
            {
              inlineData: {
                mimeType: "application/pdf",
                data: base64Data,
              },
            },
            prompt,
          ],
          config: {
            responseMimeType: "application/json",
            responseSchema: {
              type: Type.OBJECT,
              properties: {
                records: {
                  type: Type.ARRAY,
                  description: "List of all distinct transport manifests or loads extracted from the PDF pages",
                  items: {
                    type: Type.OBJECT,
                    properties: {
                      id: { type: Type.STRING, description: "Manifest or Load Number ID" },
                      date_hauled: { type: Type.STRING, description: "Date of transport/hauling if explicitly present on the sheet (e.g. YYYY-MM-DD)" },
                      vehicle_details: { type: Type.STRING, description: "Year, Make, Model description of the vehicle" },
                      vin: { type: Type.STRING, description: "The 17-character VIN number" },
                      origin: { type: Type.STRING, description: "Source or pickup city/state" },
                      destination: { type: Type.STRING, description: "Destination or delivery city/state" },
                      for_client: { type: Type.STRING, description: "Client/Company name the cargo is transported for" },
                      hand_written: { type: Type.STRING, description: "Any hand written notes, prices, or comments visible on the page" },
                      price: { type: Type.NUMBER, description: "The haul price. If a hand-written note has a dollar sign ($), that number MUST be extracted as the price. If no dollar sign is present, return the printed price, or 0 if none." },
                    },
                    required: ["id", "date_hauled", "vehicle_details", "vin", "origin", "destination", "for_client", "hand_written", "price"],
                  }
                }
              },
              required: ["records"],
            },
          },
        });

        const extractedText = response.text || "{}";
        const parsedData = JSON.parse(extractedText.trim());

        const decodedRecords = [];
        if (parsedData && Array.isArray(parsedData.records)) {
          // Parallelize the NHTSA lookups for this document's records
          const decodePromises = parsedData.records.map(async (record) => {
            const nhtsa = await fetchNhtsaData(record.vin || "");
            return {
              ...record,
              drivetrain: nhtsa.drivetrain,
              epb: nhtsa.epb,
            };
          });
          const resolvedRecords = await Promise.all(decodePromises);
          decodedRecords.push(...resolvedRecords);
        } else {
          // Fallback if parsedData itself is a single record or records is missing
          const singleRecord = parsedData.records ? parsedData.records : parsedData;
          if (singleRecord && (singleRecord.id || singleRecord.vin)) {
            const nhtsa = await fetchNhtsaData(singleRecord.vin || "");
            decodedRecords.push({
              id: singleRecord.id || "",
              date_hauled: singleRecord.date_hauled || "",
              vehicle_details: singleRecord.vehicle_details || "",
              vin: singleRecord.vin || "",
              origin: singleRecord.origin || "",
              destination: singleRecord.destination || "",
              for_client: singleRecord.for_client || "",
              hand_written: singleRecord.hand_written || "",
              price: singleRecord.price || 0,
              drivetrain: nhtsa.drivetrain,
              epb: nhtsa.epb,
            });
          }
        }

        return {
          filename: file.name,
          extracted: decodedRecords,
        };
      })
    );

    logAction("OCR PDF Processed", `Successfully parsed ${files.length} PDF file(s) via parallel Gemini OCR.`);
    res.json({ results });
  } catch (error: any) {
    console.error("OCR API Error:", error);
    res.status(500).json({ error: error.message || "An error occurred during OCR scanning." });
  }
});

// API: Apply to Sheets & Create Daily Loads
app.post("/api/submit-sheets", async (req, res) => {
  try {
    const { entries, accessToken, dateHauled } = req.body;
    if (!entries || !Array.isArray(entries) || entries.length === 0) {
       res.status(400).json({ error: "No entries provided for submission." });
       return;
    }
    if (!accessToken) {
       res.status(401).json({ error: "Missing Google Workspace authentication token." });
       return;
    }

    // Default haul date is the day of extraction (current local date) unless provided
    const haulDate = dateHauled || new Date().toISOString().split("T")[0];

    // --- Google Sheets & Drive Helper Functions inside Route Scope ---
    const headers = {
      Authorization: `Bearer ${accessToken}`,
      "Content-Type": "application/json",
    };

    // Helper: Find sheet/file by name
    async function findFileByName(name: string): Promise<any> {
      const q = `name='${name.replace(/'/g, "\\'")}' and mimeType='application/vnd.google-apps.spreadsheet' and trashed = false`;
      const url = `https://www.googleapis.com/drive/v3/files?q=${encodeURIComponent(q)}&fields=files(id,name,webViewLink)`;
      const response = await fetch(url, { headers });
      if (!response.ok) {
        throw new Error(`Failed to query Google Drive for ${name}: ${response.statusText}`);
      }
      const data = await response.json();
      return data.files && data.files.length > 0 ? data.files[0] : null;
    }

    // Helper: Create empty spreadsheet
    async function createSpreadsheet(name: string): Promise<any> {
      const url = "https://www.googleapis.com/drive/v3/files";
      const response = await fetch(url, {
        method: "POST",
        headers,
        body: JSON.stringify({
          name,
          mimeType: "application/vnd.google-apps.spreadsheet",
        }),
      });
      if (!response.ok) {
        throw new Error(`Failed to create spreadsheet ${name}: ${response.statusText}`);
      }
      return await response.json();
    }

    // Helper: Copy file
    async function copyFile(fileId: string, newName: string): Promise<any> {
      const url = `https://www.googleapis.com/drive/v3/files/${fileId}/copy`;
      const response = await fetch(url, {
        method: "POST",
        headers,
        body: JSON.stringify({ name: newName }),
      });
      if (!response.ok) {
        throw new Error(`Failed to copy file ${fileId} to ${newName}: ${response.statusText}`);
      }
      return await response.json();
    }

    // Helper: Append rows to sheet
    async function appendRows(spreadsheetId: string, range: string, rows: any[][]) {
      const url = `https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId}/values/${encodeURIComponent(range)}:append?valueInputOption=USER_ENTERED`;
      const response = await fetch(url, {
        method: "POST",
        headers,
        body: JSON.stringify({ values: rows }),
      });
      if (!response.ok) {
        throw new Error(`Failed to append rows to spreadsheet ${spreadsheetId}: ${response.statusText}`);
      }
      return await response.json();
    }

    // Helper: Clear & Update spreadsheet range
    async function updateValues(spreadsheetId: string, range: string, rows: any[][]) {
      const url = `https://sheets.googleapis.com/v4/spreadsheets/${spreadsheetId}/values/${encodeURIComponent(range)}?valueInputOption=USER_ENTERED`;
      const response = await fetch(url, {
        method: "PUT",
        headers,
        body: JSON.stringify({ values: rows }),
      });
      if (!response.ok) {
        throw new Error(`Failed to update values in spreadsheet ${spreadsheetId}: ${response.statusText}`);
      }
      return await response.json();
    }

    // --- 1. Process Jenkins_Paysheet ---
    // Look up file "Jenkins_Paysheet"
    let paysheetFile = await findFileByName("Jenkins_Paysheet");
    if (!paysheetFile) {
      // Create if it doesn't exist
      paysheetFile = await createSpreadsheet("Jenkins_Paysheet");
      // Add initial headers
      await updateValues(paysheetFile.id, "Sheet1!A1:H1", [
        [
          "Manifest #",
          "Date Hauled",
          "Vehicle Details",
          "VIN",
          "Origin",
          "Destination",
          "For / Client",
          "Price"
        ]
      ]);
    }

    // Map rows for Paysheet:
    // Manifest #, Date Hauled, Vehicle Details, VIN, Origin, Destination, For / Client, Price
    const paysheetRows = entries.map((e: any) => [
      e.id || "",
      haulDate,
      e.vehicle_details || "",
      e.vin || "",
      e.origin || "",
      e.destination || "",
      e.for_client || "",
      e.price ? parseFloat(e.price) : 0
    ]);

    await appendRows(paysheetFile.id, "Sheet1!A:A", paysheetRows);

    // --- 2. Process Daily Loads Spreadsheet ---
    // Filename format: "day of extraction_LOAD Sheet"
    // We should use the template "Dailyloads.template" as the template for creation if found.
    const extractionDay = new Date().toISOString().split("T")[0];
    const dailyLoadFileName = `${extractionDay}_LOAD Sheet`;

    // Try finding template "Dailyloads.template"
    const templateFile = await findFileByName("Dailyloads.template");
    let newDailyLoadFile;

    if (templateFile) {
      // Copy template
      const copyResult = await copyFile(templateFile.id, dailyLoadFileName);
      newDailyLoadFile = {
        id: copyResult.id,
        webViewLink: `https://docs.google.com/spreadsheets/d/${copyResult.id}/edit`
      };
    } else {
      // Create fresh if template doesn't exist yet (and set default headers)
      newDailyLoadFile = await createSpreadsheet(dailyLoadFileName);
      await updateValues(newDailyLoadFile.id, "Sheet1!A1:H1", [
        [
          "Manifest #",
          "Vehicle Details",
          "VIN",
          "Origin",
          "Destination",
          "Hand Written Notes",
          "Drivetrain",
          "EPB"
        ]
      ]);
    }

    // Map rows for Daily Loads:
    // (manifest, vehicle_details, vin, origin, destination, hand_written, drivetrain, epb)
    const dailyLoadRows = entries.map((e: any) => [
      e.id || "",
      e.vehicle_details || "",
      e.vin || "",
      e.origin || "",
      e.destination || "",
      e.hand_written || "",
      e.drivetrain || "",
      e.epb || ""
    ]);

    // Append load rows to the daily load sheet (append after the headers)
    await appendRows(newDailyLoadFile.id, "Sheet1!A:A", dailyLoadRows);

    // --- 3. Save entries & logs to Local JSON DB ---
    const db = getDatabase();
    
    // Add confirmed entries
    const savedEntries = entries.map((e: any) => ({
      ...e,
      date_hauled: haulDate,
      extracted_at: new Date().toISOString(),
      id_db: Math.random().toString(36).substring(2, 11),
    }));

    db.entries.push(...savedEntries);
    saveDatabase(db);

    // Log the action
    logAction(
      "Spreadsheets Updated",
      `Added ${entries.length} entries to 'Jenkins_Paysheet' and created '${dailyLoadFileName}' spreadsheet.`
    );

    res.json({
      success: true,
      paysheetLink: paysheetFile.webViewLink || `https://docs.google.com/spreadsheets/d/${paysheetFile.id}/edit`,
      dailyLoadLink: newDailyLoadFile.webViewLink || `https://docs.google.com/spreadsheets/d/${newDailyLoadFile.id}/edit`,
      entriesAdded: savedEntries.length,
    });
  } catch (error: any) {
    console.error("Submit Sheets Error:", error);
    res.status(500).json({ error: error.message || "An error occurred while writing data to Google Sheets." });
  }
});

// Server-side Vite middleware & static serving
async function startServer() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Server running on port ${PORT}`);
  });
}

startServer();

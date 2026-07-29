import type { DailySheet, Load } from "./types";
const DRIVE_FILES = "https://www.googleapis.com/drive/v3/files";
const SHEETS = "https://sheets.googleapis.com/v4/spreadsheets";
const aliases = {
  vehicleDetails: ["vehicle details", "vehicle", "vehicle description", "year make model"], vin: ["vin", "vehicle identification number"],
  origin: ["origin", "pickup", "pickup location"], destination: ["destination", "delivery", "delivery location"],
  notes: ["notes", "note", "hand written", "handwritten", "hand written notes", "comments"], drivetrain: ["drivetrain", "drive train", "drive type"],
  epb: ["epb", "electronic parking brake", "electric parking brake"], completed: ["completed"],
} as const;
async function googleFetch<T>(url: string, accessToken: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, { ...init, headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json", ...init?.headers } });
  if (!response.ok) { let detail = ""; try { const body = await response.json() as { error?: { message?: string } }; detail = body.error?.message ?? ""; } catch { /* non-JSON response */ }
    if (response.status === 401) throw new Error("Your Google session expired. Sign out and sign in again."); throw new Error(detail || `Google request failed (${response.status}).`); }
  return response.json() as Promise<T>;
}
const normalize = (value: string) => value.trim().toLowerCase().replace(/[_-]+/g, " ").replace(/\s+/g, " ");
function findColumn(headers: string[], names: readonly string[]) { const normalized = headers.map(normalize); return normalized.findIndex((header) => names.includes(header)); }
function columnLetter(index: number) { let value = index + 1, result = ""; while (value > 0) { value -= 1; result = String.fromCharCode(65 + value % 26) + result; value = Math.floor(value / 26); } return result; }
const cell = (row: string[], index: number) => index >= 0 ? String(row[index] ?? "").trim() : "";
const isCompleted = (value: string) => ["true", "yes", "y", "1", "completed", "done"].includes(normalize(value));

export async function getTodaySheet(accessToken: string, date: string): Promise<DailySheet> {
  const fileName = `${date}_LOAD Sheet`; const escapedName = fileName.replace(/'/g, "\\'");
  const params = new URLSearchParams({ q: `name='${escapedName}' and mimeType='application/vnd.google-apps.spreadsheet' and trashed=false`, fields: "files(id,name)", pageSize: "10" });
  const drive = await googleFetch<{ files?: Array<{ id: string; name: string }> }>(`${DRIVE_FILES}?${params}`, accessToken);
  const file = drive.files?.find((item) => item.name === fileName); if (!file) throw new Error(`Couldn’t find “${fileName}” in Google Drive.`);
  const values = await googleFetch<{ values?: string[][] }>(`${SHEETS}/${file.id}/values/${encodeURIComponent("'Sheet1'")}`, accessToken);
  const rows = values.values ?? []; if (rows.length === 0) throw new Error("Sheet1 is empty and has no header row.");
  const headers = rows[0].map(String); let completedColumn = findColumn(headers, aliases.completed);
  if (completedColumn < 0) { completedColumn = headers.length; const target = encodeURIComponent(`'Sheet1'!${columnLetter(completedColumn)}1`); await googleFetch(`${SHEETS}/${file.id}/values/${target}?valueInputOption=RAW`, accessToken, { method: "PUT", body: JSON.stringify({ values: [["Completed"]] }) }); }
  const columns = { vehicleDetails: findColumn(headers, aliases.vehicleDetails), vin: findColumn(headers, aliases.vin), origin: findColumn(headers, aliases.origin), destination: findColumn(headers, aliases.destination), notes: findColumn(headers, aliases.notes), drivetrain: findColumn(headers, aliases.drivetrain), epb: findColumn(headers, aliases.epb) };
  const loads: Load[] = rows.slice(1).flatMap((row, index) => Object.values(columns).some((column) => cell(row, column) !== "") ? [{ rowNumber: index + 2, vehicleDetails: cell(row, columns.vehicleDetails), vin: cell(row, columns.vin), origin: cell(row, columns.origin), destination: cell(row, columns.destination), notes: cell(row, columns.notes), drivetrain: cell(row, columns.drivetrain), epb: cell(row, columns.epb), completed: isCompleted(cell(row, completedColumn)) }] : []);
  return { spreadsheetId: file.id, completedColumn, loads };
}
export async function updateCompleted(accessToken: string, sheet: DailySheet, rowNumber: number, completed: boolean) { const target = encodeURIComponent(`'Sheet1'!${columnLetter(sheet.completedColumn)}${rowNumber}`); await googleFetch(`${SHEETS}/${sheet.spreadsheetId}/values/${target}?valueInputOption=USER_ENTERED`, accessToken, { method: "PUT", body: JSON.stringify({ values: [[completed]] }) }); }

import express from "express";
import path from "path";
import { createServer as createViteServer } from "vite";

const app = express();
const port = Number(process.env.PORT) || 3000;

async function start() {
  if (process.env.NODE_ENV === "production") {
    const publicDir = path.join(process.cwd(), "dist");
    app.use(express.static(publicDir));
    app.get("*", (_request, response) => response.sendFile(path.join(publicDir, "index.html")));
  } else {
    const vite = await createViteServer({ server: { middlewareMode: true }, appType: "spa" });
    app.use(vite.middlewares);
  }
  app.listen(port, "0.0.0.0", () => console.log(`Daily load app listening on http://localhost:${port}`));
}
void start();

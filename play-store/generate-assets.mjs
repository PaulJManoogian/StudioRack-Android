import fs from "node:fs/promises";
import path from "node:path";
import process from "node:process";
import { createRequire } from "node:module";

const require = createRequire(import.meta.url);
const sharp = require("sharp");

const repoRoot = path.resolve(import.meta.dirname, "..");
const outputDirectory = path.join(repoRoot, "play-store", "assets");
const defaultBrandDirectory = path.resolve(
  repoRoot,
  "..",
  "manoogianmedia.com",
  "drumdb",
  "branding",
  "studio-leviathan",
  "dist",
);
const brandDirectory = process.env.LEVIATHAN_BRAND_ASSET_DIR ?? defaultBrandDirectory;

await fs.mkdir(outputDirectory, { recursive: true });

const sourceIcon = path.join(brandDirectory, "studio-leviathan-app-icon-1024.png");
const symbol = path.join(brandDirectory, "studio-leviathan-symbol-amber.svg");

await sharp(sourceIcon)
  .resize(512, 512, { fit: "cover" })
  .png({ compressionLevel: 9 })
  .toFile(path.join(outputDirectory, "studio-leviathan-app-icon-512.png"));

const symbolPng = await sharp(symbol)
  .resize(330, 330, { fit: "contain" })
  .png()
  .toBuffer();

const featureGraphic = Buffer.from(`
  <svg width="1024" height="500" viewBox="0 0 1024 500" xmlns="http://www.w3.org/2000/svg">
    <rect width="1024" height="500" fill="#07090f"/>
    <rect x="0" y="0" width="14" height="500" fill="#ff9f0a"/>
    <path d="M410 390h102l18-35 25 72 26-112 30 75h202" fill="none" stroke="#40d4ff" stroke-width="12" stroke-linecap="round" stroke-linejoin="round"/>
    <circle cx="811" cy="390" r="8" fill="#40d4ff"/>
    <text x="420" y="145" fill="#f6f7fb" font-family="Arial, Helvetica, sans-serif" font-size="38" font-weight="700" letter-spacing="5">STUDIO</text>
    <text x="418" y="237" fill="#ff9f0a" font-family="Arial, Helvetica, sans-serif" font-size="82" font-weight="800">LEVIATHAN</text>
    <text x="422" y="292" fill="#adb4c5" font-family="Arial, Helvetica, sans-serif" font-size="21" font-weight="600" letter-spacing="2">THE PERFORMANCE OPERATIONS PLATFORM</text>
  </svg>
`);

await sharp({
  create: {
    width: 1024,
    height: 500,
    channels: 4,
    background: "#07090f",
  },
})
  .composite([
    { input: featureGraphic, left: 0, top: 0 },
    { input: symbolPng, left: 50, top: 85 },
  ])
  .png({ compressionLevel: 9 })
  .toFile(path.join(outputDirectory, "studio-leviathan-feature-graphic-1024x500.png"));

console.log(`Play artwork written to ${outputDirectory}`);

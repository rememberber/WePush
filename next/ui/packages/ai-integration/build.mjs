import { build } from "esbuild";
import { mkdir, copyFile, readFile, readdir, writeFile } from "node:fs/promises";
import { dirname, resolve } from "node:path";

await mkdir("dist", { recursive: true });
const result = await build({ entryPoints: ["src/cli.mjs"], outfile: "dist/wepush-ai.mjs", bundle: true, write: false, metafile: true,
  platform: "node", target: "node24", format: "esm", loader: { ".md": "text" },
  banner: { js: 'import { createRequire } from "node:module"; const require = createRequire(import.meta.url);' } });
const packages = new Map();
for (const input of Object.keys(result.metafile.inputs).filter((path) => path.includes("node_modules/"))) {
  let directory = dirname(resolve(input));
  while (directory !== dirname(directory)) {
    try {
      const metadata = JSON.parse(await readFile(`${directory}/package.json`, "utf8"));
      if (metadata.name && metadata.version) { packages.set(directory, metadata); break; }
    } catch (error) { if (error.code !== "ENOENT") throw error; }
    directory = dirname(directory);
  }
}
const notices = [];
for (const [directory, metadata] of [...packages].sort((a, b) => a[1].name.localeCompare(b[1].name))) {
  const files = (await readdir(directory)).filter((file) => /^(licen[sc]e|notice)(\.|$)/i.test(file)).sort();
  if (!files.length) throw new Error(`Missing bundled dependency license: ${metadata.name}`);
  notices.push(`${metadata.name} ${metadata.version}\n${(await Promise.all(files.map((file) => readFile(`${directory}/${file}`, "utf8")))).join("\n")}`);
}
// The downloaded single-file installer retains the licenses of every bundled dependency.
const licenses = notices.join("\n\n----------------------------------------\n\n");
await writeFile("dist/wepush-ai.mjs", `/*!\n${licenses.replaceAll("*/", "* /")}\n*/\n${result.outputFiles[0].text}`);
await writeFile("dist/THIRD-PARTY-LICENSES.txt", licenses);
await copyFile("skill/wepush/SKILL.md", "dist/SKILL.md");

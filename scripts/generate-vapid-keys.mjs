#!/usr/bin/env node
/**
 * Generate VAPID keys for local Web Push and optionally merge into application-local.yml.
 *
 * Usage (from repo root or backend/):
 *   node backend/scripts/generate-vapid-keys.mjs
 *   node backend/scripts/generate-vapid-keys.mjs --write
 *
 * Requires network once for: npx web-push generate-vapid-keys
 */
import { execSync } from 'node:child_process';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const backendDir = path.resolve(__dirname, '..');
const localPath = path.join(backendDir, 'application-local.yml');
const examplePath = path.join(backendDir, 'application-local.example.yml');

const writeMode = process.argv.includes('--write');

function generateVapidKeys() {
  let out;
  try {
    out = execSync('npx --yes web-push@3.6.7 generate-vapid-keys', {
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe'],
    });
  } catch (err) {
    console.error('Failed to run web-push. Ensure Node.js and npm are available.');
    console.error(err.message || err);
    process.exit(1);
  }

  const publicMatch = out.match(/Public Key:\s*\r?\n([^\r\n]+)/);
  const privateMatch = out.match(/Private Key:\s*\r?\n([^\r\n]+)/);
  if (!publicMatch || !privateMatch) {
    console.error('Unexpected web-push output:\n', out);
    process.exit(1);
  }

  return {
    publicKey: publicMatch[1].trim(),
    privateKey: privateMatch[1].trim(),
  };
}

function pushYamlBlock(keys) {
  return `  push:
    web:
      enabled: true
      vapid-public-key: "${keys.publicKey}"
      vapid-private-key: "${keys.privateKey}"
      vapid-subject: "mailto:support@youme.app"
`;
}

function mergeIntoYaml(content, keys) {
  const block = pushYamlBlock(keys);

  if (/vapid-public-key:/m.test(content)) {
    let next = content.replace(
      /vapid-public-key:\s*("?)[^"\n]*("?)/,
      `vapid-public-key: "${keys.publicKey}"`,
    );
    next = next.replace(
      /vapid-private-key:\s*("?)[^"\n]*("?)/,
      `vapid-private-key: "${keys.privateKey}"`,
    );
    next = next.replace(
      /(push:\s*\r?\n\s+web:\s*\r?\n\s+)enabled:\s*false/,
      '$1enabled: true',
    );
    return next;
  }

  if (/^app:/m.test(content)) {
    if (/^  push:/m.test(content)) {
      return content;
    }
    return content.replace(/^app:\s*\r?\n/m, `app:\n${block}`);
  }

  const trimmed = content.trimEnd();
  return `${trimmed}\n\napp:\n${block}`;
}

function ensureLocalFile() {
  if (fs.existsSync(localPath)) {
    return fs.readFileSync(localPath, 'utf8');
  }
  if (fs.existsSync(examplePath)) {
    console.log(`Creating ${path.relative(process.cwd(), localPath)} from example template…`);
    return fs.readFileSync(examplePath, 'utf8');
  }
  console.log(`Creating minimal ${path.relative(process.cwd(), localPath)}…`);
  return `spring:
  datasource:
    password: your-postgres-password

app:
  jwt:
    secret: "change-me-run-openssl-rand-base64-48"
`;
}

function main() {
  const keys = generateVapidKeys();

  console.log('\n=== VAPID keys (Web Push) ===\n');
  console.log('Public Key:\n', keys.publicKey);
  console.log('\nPrivate Key:\n', keys.privateKey);
  console.log('\n--- Paste under app: in application-local.yml ---\n');
  console.log(pushYamlBlock(keys));

  if (!writeMode) {
    console.log('To write these into backend/application-local.yml automatically, run:\n');
    console.log('  node backend/scripts/generate-vapid-keys.mjs --write\n');
    return;
  }

  const original = ensureLocalFile();
  const merged = mergeIntoYaml(original, keys);
  fs.writeFileSync(localPath, merged, 'utf8');

  console.log(`Updated: ${path.relative(process.cwd(), localPath)}`);
  console.log('\nNext steps:');
  console.log('  1. Restart Spring Boot (backend)');
  console.log('  2. Apply migration V17 if not done (device_tokens.token → TEXT)');
  console.log('  3. Profile → Enable push in the app (allow browser notifications)');
  console.log('  4. Send a test message from another account while the tab is in the background\n');
}

main();

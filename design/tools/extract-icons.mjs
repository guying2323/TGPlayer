#!/usr/bin/env node
/**
 * Regenerates design/icons.json from the react-icons package.
 *
 *   npm install react-icons@5.7.0      # in a scratch dir
 *   REACT_ICONS_DIR=/path/to/node_modules/react-icons node design/tools/extract-icons.mjs
 *
 * react-icons ships React components; we only take the SVG geometry each
 * GenIcon() call carries, so the same definition feeds the HTML preview and
 * the generated Compose ImageVectors.
 */
import fs from 'node:fs';
import path from 'node:path';

const pkgDir = process.env.REACT_ICONS_DIR || 'node_modules/react-icons';
const outFile = process.env.OUT || path.join(import.meta.dirname, '..', 'icons.json');

// Public icon id -> component name in the pack.
const WANT = {
  fi: {
    home: 'FiHome', chat: 'FiMessageSquare', settings: 'FiSettings', search: 'FiSearch',
    play: 'FiPlay', pause: 'FiPause', back: 'FiChevronLeft', forward: 'FiChevronRight',
    down: 'FiChevronDown', cast: 'FiCast', lock: 'FiLock', next: 'FiSkipForward',
    volume: 'FiVolume2', offline: 'FiWifiOff', download: 'FiDownload', more: 'FiMoreVertical',
    refresh: 'FiRefreshCw', archive: 'FiArchive', close: 'FiX', fullscreen: 'FiMaximize',
    clock: 'FiClock', user: 'FiUser', folder: 'FiFolder', sliders: 'FiSliders',
    star: 'FiStar', check: 'FiCheck', minus: 'FiMinus', inbox: 'FiInbox',
    globe: 'FiGlobe', logout: 'FiLogOut', tv: 'FiTv', plus: 'FiPlus',
  },
  lu: {
    captions: 'LuCaptions', gauge: 'LuGauge', 'list-video': 'LuListVideo',
    'screen-play': 'LuMonitorPlay', language: 'LuLanguages', tune: 'LuSettings2',
    pip: 'LuPictureInPicture2',
  },
};

function extract(file) {
  const src = fs.readFileSync(file, 'utf8');
  const out = new Map();
  const re = /export function (\w+) \(props\) \{\s*return GenIcon\(/g;
  let m;
  while ((m = re.exec(src))) {
    let i = re.lastIndex - 1;
    let depth = 0, start = i + 1, end = -1;
    for (; i < src.length; i++) {
      const c = src[i];
      if (c === '(') depth++;
      else if (c === ')' && --depth === 0) { end = i; break; }
    }
    if (end < 0) continue;
    try { out.set(m[1], JSON.parse(src.slice(start, end))); } catch { /* skip malformed */ }
  }
  return out;
}

const pkg = JSON.parse(fs.readFileSync(path.join(pkgDir, 'package.json'), 'utf8'));
const icons = {};
const missing = [];
for (const [pack, names] of Object.entries(WANT)) {
  const all = extract(path.join(pkgDir, pack, 'index.mjs'));
  for (const [id, component] of Object.entries(names)) {
    if (all.has(component)) icons[id] = { pack, component, ...all.get(component) };
    else missing.push(component);
  }
}

const result = {
  source: 'react-icons',
  version: pkg.version,
  license: 'MIT (react-icons); Feather MIT; Lucide ISC',
  generatedFor: 'TMPlayer Android design/ui-preview.html and ui/theme/AppIcons.kt',
  icons,
};
fs.writeFileSync(outFile, JSON.stringify(result, null, 1));
console.log('react-icons', pkg.version, '->', Object.keys(icons).length, 'icons');
if (missing.length) console.warn('missing:', missing.join(', '));

#!/usr/bin/env bash
# Regenerates the raster app icons from the SVG sources in this folder (ADR 0001, item 9.13 of the roadmap).
# Needs Google Chrome (SVG rendering), ImageMagick (masks, sizes, .ico) and macOS iconutil (.icns).
# Android uses vector drawables (androidApp/src/main/res/drawable/ic_launcher_*.xml), so it is not generated here.
set -euo pipefail
cd "$(dirname "$0")"
ROOT=../..
CHROME="${CHROME:-/Applications/Google Chrome.app/Contents/MacOS/Google Chrome}"
TMP=$(mktemp -d)
trap 'rm -rf "$TMP"' EXIT

render() { # svg size out
  printf '<html><body style="margin:0"><img src="file://%s" width="%s" height="%s" style="display:block"></body></html>' \
    "$PWD/$1" "$2" "$2" > "$TMP/page.html"
  "$CHROME" --headless=new --disable-gpu --hide-scrollbars --window-size="$2,$2" --screenshot="$3" "file://$TMP/page.html" >/dev/null 2>&1
}

# iOS: one 1024 px image per appearance (default, dark, tinted).
IOS="$ROOT/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset"
render icon.svg 1024 "$IOS/app-icon-1024.png"
render icon-dark.svg 1024 "$IOS/app-icon-dark-1024.png"
render icon-tinted.svg 1024 "$IOS/app-icon-tinted-1024.png"

# Desktop and window/tray: macOS-style rounded square with a transparent margin.
render icon.svg 1024 "$TMP/square.png"
magick -size 824x824 xc:black -fill white -draw "roundrectangle 0,0,823,823,185,185" "$TMP/mask.png"
magick "$TMP/square.png" -resize 824x824 "$TMP/inner.png"
magick "$TMP/inner.png" "$TMP/mask.png" -alpha off -compose CopyOpacity -composite \
  -compose over -background none -gravity center -extent 1024x1024 -define png:color-type=6 "$TMP/rounded.png"
magick "$TMP/rounded.png" -resize 512x512 -define png:color-type=6 "$ROOT/shared/src/commonMain/composeResources/drawable/app_icon.png"
mkdir -p "$ROOT/desktopApp/icons" "$TMP/icon.iconset"
magick "$TMP/rounded.png" -resize 512x512 -define png:color-type=6 "$ROOT/desktopApp/icons/icon.png"
for s in 16 32 128 256 512; do
  magick "$TMP/rounded.png" -resize "${s}x${s}" "$TMP/icon.iconset/icon_${s}x${s}.png"
  magick "$TMP/rounded.png" -resize "$((s * 2))x$((s * 2))" "$TMP/icon.iconset/icon_${s}x${s}@2x.png"
done
iconutil -c icns "$TMP/icon.iconset" -o "$ROOT/desktopApp/icons/icon.icns"
magick "$TMP/rounded.png" -define icon:auto-resize=256,64,48,32,16 "$ROOT/desktopApp/icons/icon.ico"

# Web: SVG favicon is used as is; PNGs for the home screen and the manifest.
WEB="$ROOT/webApp/src/wasmJsMain/resources"
cp favicon.svg "$WEB/favicon.svg"
magick "$TMP/square.png" -resize 180x180 "$WEB/apple-touch-icon.png"
magick "$TMP/square.png" -resize 192x192 "$WEB/icon-192.png"
magick "$TMP/square.png" -resize 512x512 "$WEB/icon-512.png"
echo "Icons generated."

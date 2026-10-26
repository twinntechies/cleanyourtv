#!/usr/bin/env bash
# Build Wi-Fi Control, which lets the launcher's Wi-Fi dropdown (wifi_dropdown) switch Wi-Fi and join networks
# itself, install it on the TV and grant the location permission Android wants before it lists saved networks.
#
# Usage: TV_HOST=192.168.1.50 addons/wifi-control/build-and-install.sh
#        TV_HOST=192.168.1.50 addons/wifi-control/build-and-install.sh --uninstall
set -euo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
TOOLS="$HERE/../../tools"
: "${TV_HOST:?set TV_HOST to the TV IP address, e.g. TV_HOST=192.168.1.50 $0}"
PY="$TOOLS/venv/bin/python"
[ -x "$PY" ] || PY="$TOOLS/venv/Scripts/python.exe"
[ -x "$PY" ] || { echo "run: python3 -m venv tools/venv && tools/venv/bin/pip install -r tools/requirements.txt" >&2; exit 1; }
export MSYS_NO_PATHCONV=1
PKG=com.gathin.wificontrol
tv() { "$PY" -I "$TOOLS/tv.py" "$@"; }

if [ "${1:-}" = --uninstall ]; then
  tv "pm uninstall $PKG"
  exit 0
fi

(cd "$HERE" && ./gradlew assembleRelease -q) || { echo "build failed (JDK 17 and Android SDK 34 needed; see launcher/README)" >&2; exit 1; }
"$PY" -I "$TOOLS/b64push.py" "$HERE/app/build/outputs/apk/release/app-release.apk" /data/local/tmp/wificontrol.apk >/dev/null
tv "pm install -r /data/local/tmp/wificontrol.apk; rm -f /data/local/tmp/wificontrol.apk"
tv "pm grant $PKG android.permission.ACCESS_FINE_LOCATION"
echo "Installed. The launcher's Wi-Fi dropdown now switches Wi-Fi and joins networks itself."

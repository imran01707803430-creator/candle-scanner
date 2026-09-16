# Candle Signal Scanner — Book Rules Edition

Android Studio project for a screen-based candlestick scanner.

## What is included
- One-tap Android screen capture permission and scan.
- Detects colored candlesticks from the chart area.
- Rule engine includes pattern directions confirmed from the supplied scanned PDF, including chart-pattern direction references and candlestick patterns such as Hammer, Inverted Hammer, Shooting Star, Doji/Dragonfly Doji, Rising/Falling Three Methods, Morning/Evening Star (approximate), Bullish/Bearish Engulfing (approximate), One White Soldier, One Black Crow, and related structures.
- Returns UP / DOWN / WAIT plus confidence and explanation.

## Important
The supplied PDF is a 186-page scan. Its text layer is essentially unavailable, so rules must be transcribed/verified from page images. This build only encodes rules that were visibly confirmed during the current build pass (not every one of the 186 pages). The image detector is heuristic and should not be treated as a guaranteed market predictor.

## Build
Open the folder in Android Studio and build the app. No external libraries are required.

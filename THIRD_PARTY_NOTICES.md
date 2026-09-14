# Third-party notices

## Photon (Komoot)

ui-lab uses Photon for reverse geocoding (coordinates → place name):

- Project: Photon
- Repository: https://github.com/komoot/photon
- Endpoint used: https://photon.komoot.io/reverse
- License: Apache License 2.0 (server); data © OpenStreetMap contributors
  under the Open Database License (ODbL)

Photon powers the "locate to street level" enhancement: when the system
geocoder only resolves a location to city level, ui-lab queries Photon to
fill in district / street / feature detail. Failures degrade silently to the
system geocoder result.

## Liquid-Glass-Android

ui-lab uses the following open-source liquid-glass implementation:

- Project: Liquid-Glass-Android
- Repository: https://github.com/QWEA0/Liquid-Glass-Android
- Version: v2.0.10
- License: MIT License

The library is used through its published Android artifact:

```text
com.github.QWEA0:liquidglass:v2.0.10
```

The original authors retain copyright in the original project. ui-lab
maintains the Compose adapter and application-specific integration code.

### MIT License

Copyright (c) the Liquid-Glass-Android authors.

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

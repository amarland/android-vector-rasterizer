# android-vector-rasterizer

A JVM command-line tool to generate WebP images from SVG files for the multiple Android pixel densities.

## Usage

Usage information can be obtained by passing the `--help / -h` option:

```
rasterize [options] <source>...

Density options:

  You can enable or disable generation of a version for a specific pixel
  density by specifying one or more of the options below.

  --ldpi / --no-ldpi          low (default: disabled)
  --mdpi / --no-mdpi          medium (default: enabled)
  --hdpi / --no-hdpi          high (default: enabled)
  --xhdpi / --no-xhdpi        extra-high (default: enabled)
  --xxhdpi / --no-xxhdpi      extra-extra-high (default: enabled)
  --xxxhdpi / --no-xxxhdpi    extra-extra-extra-high (default: enabled)

Dimension options:

  The desired width and height (in density-independent pixels) of the
  generated images can be set via the options below.
  If not set explicitly, the size will be determined by the 'width' and
  'height' attributes of the 'svg' element, or by the 'viewBox' attribute if
  these are not set.
  If only one of the two is set, then the other one will be computed with
  respect to the original aspect ratio.

  --width <float>     Width in dp.
  --height <float>    Height in dp.

Options:

  -d, --destination <dir>    Location of the generated WebP files
                             (must be a directory).
```
# android-vector-rasterizer

A JVM command-line tool to generate WebP images from SVG files for the multiple Android pixel densities.

## Usage

Usage information can be obtained by passing the `--help / -h` option:

```
rasterize [OPTIONS] SOURCE...

Density options:

  Enable/disable generation of a version for a specific pixel density

  --ldpi / --no-ldpi         low (default: disabled)
  --mdpi / --no-mdpi         medium (default: enabled)
  --hdpi / --no-hdpi         high (default: enabled)
  --xhdpi / --no-xhdpi       extra-high (default: enabled)
  --xxhdpi / --no-xxhdpi     extra-extra-high (default: enabled)
  --xxxhdpi / --no-xxxhdpi   extra-extra-extra-high (default: enabled)

Options:
  -d, --destination DEST   Set the location of the generated WebP files
                           (must be a directory)
  -h, --help               Show this message and exit
```
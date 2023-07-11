# android-vector-rasterizer ![Build](https://github.com/amarland/android-vector-rasterizer/actions/workflows/gradle-ci.yml/badge.svg)

A command-line tool to generate WebP images from SVG files for the multiple Android pixel densities.

**Java 11 or later is required to run this tool.**

## Usage

Usage information can be obtained by passing the `--help / -h` option:

```
java -jar android-vector-rasterizer.jar [<options>] <source>...

Density options:

  You can enable or disable generation of a version for a specific pixel
  density by specifying one or more of the options below.

  --ldpi / --no-ldpi        (default: not generated)
  --mdpi / --no-mdpi        (default: generated)
  --hdpi / --no-hdpi        (default: generated)
  --xhdpi / --no-xhdpi      (default: generated)
  --xxhdpi / --no-xxhdpi    (default: generated)
  --xxxhdpi / --no-xxxhdpi  (default: generated)

Dimension options:

  The desired width and height (in density-independent pixels) of the generated
  images can be set via the options below.
  If not set explicitly, the size will be determined by the 'width' and
  'height' attributes of the 'svg' element, or by the 'viewBox' attribute if
  these are not set.
  If only one of the two is set, then the other one will be computed with
  respect to the original aspect ratio.

  --width=<int>   Width in dp.
  --height=<int>  Height in dp.

Options:
  -d, --destination=<dir>  Location of the generated WebP files
                           (must be a directory).
  --force-transparent-white  Convert transparent black (#00000000) pixels
                             to transparent white pixels (#00FFFFFF).
```

### Examples:

- Rasterize `vector.svg` and generate WebP versions for all densities (all but `ldpi` are implied if not explicitly unset):\
  ```java -jar android-vector-rasterizer.jar --ldpi vector.svg```

- Set a target width of 256 density-independent pixels (while preserving the aspect ratio):\
  ```java -jar android-vector-rasterizer.jar --width=256 vector.svg```

- Specify the location of the generated files:\
  ```java -jar android-vector-rasterizer.jar -d generated/ vector1.svg vector2.svg vector3.svg```

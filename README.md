[![NPM Version][npm-image]][npm-url]
[![NPM Downloads][downloads-image]][downloads-url]

# Capacitor Canvas Camera plugin

## Plugin's Purpose

The purpose of the plugin is to capture video to preview camera in a web page's canvas element.
Allows to select front or back camera and to control the flash.

## Install

```bash
npm install @jewel998/camera-preview
npx cap sync
```

## API

<docgen-index>

* [`init(...)`](#init)
* [`start()`](#start)
* [`stop()`](#stop)
* [`setOrientationChange(...)`](#setorientationchange)
* [`flip()`](#flip)
* [`getSupportedFlashModes()`](#getsupportedflashmodes)
* [`setFlashMode(...)`](#setflashmode)
* [`onRenderFrame(...)`](#onrenderframe)
* [Type Aliases](#type-aliases)

</docgen-index>

<docgen-api>
<!--Update the source file JSDoc comments and rerun docgen to update the docs below-->

### init(...)

```typescript
init(options: CameraInitOptions) => void
```

| Param         | Type                                                            |
| ------------- | --------------------------------------------------------------- |
| **`options`** | <code><a href="#camerainitoptions">CameraInitOptions</a></code> |

--------------------


### start()

```typescript
start() => Promise<void>
```

--------------------


### stop()

```typescript
stop() => Promise<void>
```

--------------------


### setOrientationChange(...)

```typescript
setOrientationChange(option: { value: 'portrait' | 'landscape'; }) => Promise<void>
```

| Param        | Type                                               |
| ------------ | -------------------------------------------------- |
| **`option`** | <code>{ value: 'portrait' \| 'landscape'; }</code> |

--------------------


### flip()

```typescript
flip() => Promise<void>
```

--------------------


### getSupportedFlashModes()

```typescript
getSupportedFlashModes() => Promise<{ result: string[]; }>
```

**Returns:** <code>Promise&lt;{ result: string[]; }&gt;</code>

--------------------


### setFlashMode(...)

```typescript
setFlashMode(option: { value: boolean; }) => Promise<void>
```

| Param        | Type                             |
| ------------ | -------------------------------- |
| **`option`** | <code>{ value: boolean; }</code> |

--------------------


### onRenderFrame(...)

```typescript
onRenderFrame(cb: RenderFrameCallback) => void
```

| Param    | Type                                                                |
| -------- | ------------------------------------------------------------------- |
| **`cb`** | <code><a href="#renderframecallback">RenderFrameCallback</a></code> |

--------------------


### Type Aliases


#### CameraInitOptions

<code>{ flashMode?: boolean; cameraFacing?: 'front' | 'rear'; fps?: number; width?: number; height?: number; canvas?: { width: number; height: number }; capture?: { width: number; height: number }; }</code>


#### RenderFrameCallback

<code>(frame: <a href="#frame">Frame</a>): unknown</code>


#### Frame

<code>{ data: string; width: number; height: number; timestamp: number }</code>

</docgen-api>

[npm-image]: https://img.shields.io/npm/v/@jewel998/camera-preview.svg
[npm-url]: https://www.npmjs.com/package/@jewel998/camera-preview
[downloads-image]: https://img.shields.io/npm/dm/@jewel998/camera-preview.svg
[downloads-url]: https://www.npmjs.com/package/@jewel998/camera-preview
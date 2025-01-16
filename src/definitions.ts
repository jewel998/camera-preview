export type CameraInitOptions = {
  flashMode?: boolean;
  cameraFacing?: 'front' | 'rear';
  fps?: number;
  width?: number;
  height?: number;
  canvas?: { width: number; height: number };
  capture?: { width: number; height: number };
}

export type Frame = { data: string; width: number; height: number; timestamp: number };
export type RenderFrameCallback = (frame: Frame) => unknown;

export interface CameraPlugin {
  init(options: CameraInitOptions): void;
  start(): Promise<void>;
  stop(): Promise<void>;
  setOrientationChange(option: { value: 'portrait' | 'landscape' }): Promise<void>;
  flip(): Promise<void>;
  getSupportedFlashModes(): Promise<{ result: string[] }>;
  setFlashMode(option: { value: boolean }): Promise<void>;
  onRenderFrame(cb: RenderFrameCallback): void;
}

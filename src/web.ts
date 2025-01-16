import { WebPlugin } from '@capacitor/core';

import type { CameraInitOptions, ICameraPlugin, RenderFrameCallback } from './definitions';

export class CameraWeb extends WebPlugin implements ICameraPlugin {
  private options!: Required<CameraInitOptions>;
  private render?: RenderFrameCallback;
  private video?: HTMLVideoElement;
  private canvas?: HTMLCanvasElement;
  private stream?: MediaStream;
  private isPreviewing = false;

  constructor() {
    super();
    this.setDefaultOptions();
  }

  public initialize(options: CameraInitOptions): void {
    this.options = { ...this.options, ...options };
  }

  public async start(): Promise<void> {
    this.stream = await navigator.mediaDevices.getUserMedia({
      audio: false,
      video: {
        facingMode: 'user',
        width: this.options.width,
        height: this.options.height,
        aspectRatio: this.options.width / this.options.height,
        autoGainControl: true,
        echoCancellation: true,
        frameRate: this.options.fps,
        noiseSuppression: true,
      }
    });
    
    const video = document.createElement('video');
    video.style.display = 'none';
    video.muted = true;
    video.srcObject = this.stream;
    video.height = this.options.height;
    video.width = this.options.width;
    await video.play();
    this.video = video;
    this.canvas = document.createElement('canvas');
    this.isPreviewing = true;
    requestAnimationFrame(this.processFrame);
  }

  private processFrame = () => {
    if (this.canvas && this.video) {
      const ctx = this.canvas.getContext('2d');
      if (ctx) {
        const { width, height } = this.video;
        this.canvas.width = width;
        this.canvas.height = height;
        ctx.drawImage(this.video, 0, 0, width, height);
        const dataUri = this.canvas.toDataURL('image/jpeg', 1);
        if (this.render) {
          this.render({ data: dataUri, width, height, timestamp: Date.now() });
        }
      }

      if (this.isPreviewing) {
        requestAnimationFrame(this.processFrame);
      }
    }
  }

  public async stop(): Promise<void> {
    this.canvas = undefined;
    this.video = undefined;
    this.isPreviewing = false;
    this.stream?.getTracks().forEach((e) => e.stop());
    this.stream = undefined;
  }

  public setOrientationChange(): Promise<void> {
    throw new Error('Method not implemented on web!');
  }

  public flip(): Promise<void> {
    throw new Error('Method not implemented on web!');
  }

  public getSupportedFlashModes(): Promise<{ result: string[]; }> {
    throw new Error('Method not supported on web!');
  }

  public setFlashMode(): Promise<void> {
    throw new Error('Method not supported on web!');
  }

  public onRenderFrame(cb: RenderFrameCallback): void {
    this.render = cb;
  }

  private setDefaultOptions() {
    this.options = {
      flashMode: false,
      fps: 30,
      cameraFacing: 'rear',
      width: window.screen.availWidth,
      height: window.screen.availHeight,
      canvas: {
        width: window.screen.availWidth,
        height: window.screen.availHeight,
      },
      capture: {
        width: window.screen.availWidth,
        height: window.screen.availHeight,
      }
    }
  }
}

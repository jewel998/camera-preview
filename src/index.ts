import { registerPlugin } from '@capacitor/core';

import type { CameraPlugin } from './definitions';

const CanvasCamera = registerPlugin<CameraPlugin>('CanvasCamera', {
  web: () => import('./web').then((m) => new m.CameraWeb()),
});

export * from './definitions';
export { CanvasCamera };

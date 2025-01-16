import { registerPlugin } from '@capacitor/core';

import type { CameraPlugin } from './definitions';

const CameraPreview = registerPlugin<CameraPlugin>('CameraPreview', {
  web: () => import('./web').then((m) => new m.CameraWeb()),
});

export * from './definitions';
export { CameraPreview };

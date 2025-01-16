import { registerPlugin } from '@capacitor/core';

import type { ICameraPlugin } from './definitions';

const CameraPlugin = registerPlugin<ICameraPlugin>('CameraPlugin', {
  web: () => import('./web').then((m) => new m.CameraWeb()),
});

export * from './definitions';
export { CameraPlugin };

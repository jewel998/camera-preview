import { WebPlugin } from '@capacitor/core';

import type { CameraPlugin } from './definitions';

export class CameraWeb extends WebPlugin implements CameraPlugin {
  async echo(options: { value: string }): Promise<{ value: string }> {
    console.log('ECHO', options);
    return options;
  }
}

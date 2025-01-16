export interface CameraPlugin {
  echo(options: { value: string }): Promise<{ value: string }>;
}

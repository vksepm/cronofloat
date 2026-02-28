import {NativeModules} from 'react-native';

interface OverlayConfig {
  is24h: boolean;
  showSeconds: boolean;
}

interface OverlayModuleInterface {
  checkPermission(): Promise<boolean>;
  requestPermission(): void;
  startOverlay(config: OverlayConfig): void;
  stopOverlay(): void;
  updateConfig(config: OverlayConfig): void;
}

const {OverlayModule} = NativeModules;

export default OverlayModule as OverlayModuleInterface;
export type {OverlayConfig};

import { registerWebModule, NativeModule } from 'expo';

import { LetroPassportModuleEvents } from './LetroPassport.types';

class LetroPassportModule extends NativeModule<LetroPassportModuleEvents> {
  PI = Math.PI;
  async setValueAsync(value: string): Promise<void> {
    this.emit('onChange', { value });
  }
  hello() {
    return 'Hello world! 👋';
  }
}

export default registerWebModule(LetroPassportModule);

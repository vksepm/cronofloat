import React, {
  createContext,
  useContext,
  useState,
  useEffect,
  useCallback,
  type ReactNode,
} from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';

interface Settings {
  is24h: boolean;
  showSeconds: boolean;
  isActive: boolean;
}

interface SettingsContextType extends Settings {
  toggle24h: () => void;
  toggleSeconds: () => void;
  setIsActive: (active: boolean) => void;
  loaded: boolean;
}

const STORAGE_KEY = '@chronofloat_settings';

const defaults: Settings = {
  is24h: true,
  showSeconds: false,
  isActive: false,
};

const SettingsContext = createContext<SettingsContextType>({
  ...defaults,
  toggle24h: () => {},
  toggleSeconds: () => {},
  setIsActive: () => {},
  loaded: false,
});

export function SettingsProvider({children}: {children: ReactNode}) {
  const [settings, setSettings] = useState<Settings>(defaults);
  const [loaded, setLoaded] = useState(false);

  useEffect(() => {
    AsyncStorage.getItem(STORAGE_KEY).then(raw => {
      if (raw) {
        try {
          const parsed = JSON.parse(raw);
          setSettings(prev => ({...prev, ...parsed}));
        } catch {}
      }
      setLoaded(true);
    });
  }, []);

  const persist = useCallback((next: Settings) => {
    setSettings(next);
    AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(next));
  }, []);

  const toggle24h = useCallback(() => {
    setSettings(prev => {
      const next = {...prev, is24h: !prev.is24h};
      AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      return next;
    });
  }, []);

  const toggleSeconds = useCallback(() => {
    setSettings(prev => {
      const next = {...prev, showSeconds: !prev.showSeconds};
      AsyncStorage.setItem(STORAGE_KEY, JSON.stringify(next));
      return next;
    });
  }, []);

  const setIsActive = useCallback(
    (active: boolean) => {
      persist({...settings, isActive: active});
    },
    [settings, persist],
  );

  return (
    <SettingsContext.Provider
      value={{...settings, toggle24h, toggleSeconds, setIsActive, loaded}}>
      {children}
    </SettingsContext.Provider>
  );
}

export function useSettings() {
  return useContext(SettingsContext);
}

import React, {useEffect, useState, useCallback} from 'react';
import {
  SafeAreaView,
  StyleSheet,
  Text,
  View,
  Switch,
  TouchableOpacity,
  AppState,
  StatusBar,
  useColorScheme,
} from 'react-native';
import {SettingsProvider, useSettings} from './context/SettingsContext';
import OverlayModule from './native/OverlayModule';

function MainScreen() {
  const {is24h, showSeconds, isActive, toggle24h, toggleSeconds, setIsActive, loaded} =
    useSettings();
  const [hasPermission, setHasPermission] = useState(false);
  const isDark = useColorScheme() === 'dark';

  const colors = {
    bg: isDark ? '#121212' : '#F5F5F5',
    card: isDark ? '#1E1E1E' : '#FFFFFF',
    text: isDark ? '#E0E0E0' : '#212121',
    textSecondary: isDark ? '#9E9E9E' : '#757575',
    accent: '#6C63FF',
    border: isDark ? '#2C2C2C' : '#E0E0E0',
  };

  const checkPerm = useCallback(() => {
    OverlayModule.checkPermission().then(setHasPermission);
  }, []);

  useEffect(() => {
    checkPerm();
    const sub = AppState.addEventListener('change', state => {
      if (state === 'active') {
        checkPerm();
      }
    });
    return () => sub.remove();
  }, [checkPerm]);

  const handleToggle = useCallback(
    (value: boolean) => {
      if (value) {
        if (!hasPermission) {
          return;
        }
        OverlayModule.startOverlay({is24h, showSeconds});
      } else {
        OverlayModule.stopOverlay();
      }
      setIsActive(value);
    },
    [hasPermission, is24h, showSeconds, setIsActive],
  );

  // When format settings change while overlay is active, update the running service
  useEffect(() => {
    if (isActive && hasPermission && loaded) {
      OverlayModule.updateConfig({is24h, showSeconds});
    }
  }, [is24h, showSeconds, isActive, hasPermission, loaded]);

  if (!loaded) {
    return null;
  }

  return (
    <SafeAreaView style={[styles.container, {backgroundColor: colors.bg}]}>
      <StatusBar
        barStyle={isDark ? 'light-content' : 'dark-content'}
        backgroundColor={colors.bg}
      />

      {/* Header */}
      <View style={styles.header}>
        <Text style={[styles.title, {color: colors.text}]}>ChronoFloat</Text>
        <Text style={[styles.subtitle, {color: colors.textSecondary}]}>
          Floating clock overlay
        </Text>
      </View>

      {/* Permission Card */}
      {!hasPermission && (
        <View style={[styles.card, {backgroundColor: colors.card, borderColor: colors.border}]}>
          <Text style={[styles.cardTitle, {color: colors.text}]}>
            Permission Required
          </Text>
          <Text style={[styles.cardBody, {color: colors.textSecondary}]}>
            ChronoFloat needs the "Display over other apps" permission to show
            the floating clock.
          </Text>
          <TouchableOpacity
            style={[styles.button, {backgroundColor: colors.accent}]}
            onPress={() => OverlayModule.requestPermission()}
            activeOpacity={0.8}>
            <Text style={styles.buttonText}>Grant Permission</Text>
          </TouchableOpacity>
        </View>
      )}

      {/* Overlay Toggle */}
      <View style={[styles.card, {backgroundColor: colors.card, borderColor: colors.border}]}>
        <View style={styles.row}>
          <View style={styles.rowText}>
            <Text style={[styles.rowTitle, {color: colors.text}]}>
              Enable Overlay
            </Text>
            <Text style={[styles.rowSubtitle, {color: colors.textSecondary}]}>
              {isActive ? 'Clock is visible' : 'Clock is hidden'}
            </Text>
          </View>
          <Switch
            value={isActive}
            onValueChange={handleToggle}
            disabled={!hasPermission}
            trackColor={{false: colors.border, true: colors.accent + '80'}}
            thumbColor={isActive ? colors.accent : isDark ? '#666' : '#CCC'}
          />
        </View>
      </View>

      {/* Settings */}
      <View style={[styles.card, {backgroundColor: colors.card, borderColor: colors.border}]}>
        <Text style={[styles.cardTitle, {color: colors.text}]}>Settings</Text>

        <View style={[styles.row, styles.rowBorder, {borderBottomColor: colors.border}]}>
          <View style={styles.rowText}>
            <Text style={[styles.rowTitle, {color: colors.text}]}>
              24-hour format
            </Text>
            <Text style={[styles.rowSubtitle, {color: colors.textSecondary}]}>
              {is24h ? '14:30' : '2:30 PM'}
            </Text>
          </View>
          <Switch
            value={is24h}
            onValueChange={toggle24h}
            trackColor={{false: colors.border, true: colors.accent + '80'}}
            thumbColor={is24h ? colors.accent : isDark ? '#666' : '#CCC'}
          />
        </View>

        <View style={styles.row}>
          <View style={styles.rowText}>
            <Text style={[styles.rowTitle, {color: colors.text}]}>
              Show seconds
            </Text>
            <Text style={[styles.rowSubtitle, {color: colors.textSecondary}]}>
              {showSeconds ? 'Updates every second' : 'Updates every minute'}
            </Text>
          </View>
          <Switch
            value={showSeconds}
            onValueChange={toggleSeconds}
            trackColor={{false: colors.border, true: colors.accent + '80'}}
            thumbColor={showSeconds ? colors.accent : isDark ? '#666' : '#CCC'}
          />
        </View>
      </View>

      {/* Info */}
      <Text style={[styles.infoText, {color: colors.textSecondary}]}>
        Tip: Drag the clock to reposition it on screen.{'\n'}No internet access
        — your privacy is respected.
      </Text>
    </SafeAreaView>
  );
}

export default function App() {
  return (
    <SettingsProvider>
      <MainScreen />
    </SettingsProvider>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    paddingHorizontal: 20,
  },
  header: {
    paddingTop: 40,
    paddingBottom: 24,
  },
  title: {
    fontSize: 28,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  subtitle: {
    fontSize: 14,
    marginTop: 4,
  },
  card: {
    borderRadius: 12,
    padding: 16,
    marginBottom: 16,
    borderWidth: 1,
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
  },
  cardBody: {
    fontSize: 14,
    lineHeight: 20,
    marginBottom: 16,
  },
  button: {
    borderRadius: 8,
    paddingVertical: 12,
    alignItems: 'center',
  },
  buttonText: {
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '600',
  },
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 4,
  },
  rowBorder: {
    borderBottomWidth: 1,
    paddingBottom: 12,
    marginBottom: 12,
  },
  rowText: {
    flex: 1,
    marginRight: 16,
  },
  rowTitle: {
    fontSize: 15,
    fontWeight: '500',
  },
  rowSubtitle: {
    fontSize: 13,
    marginTop: 2,
  },
  infoText: {
    fontSize: 12,
    lineHeight: 18,
    textAlign: 'center',
    marginTop: 8,
    paddingHorizontal: 16,
  },
});

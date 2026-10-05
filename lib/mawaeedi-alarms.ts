import { registerPlugin } from '@capacitor/core'

type Alarm = { id: string; triggerAtMillis: number; title: string }

type MawaeediAlarmsPlugin = {
  schedule(options: Alarm): Promise<{ requiresExactAlarmPermission?: boolean }>
  cancel(options: { id: string }): Promise<void>
  requestExactAlarmPermission(): Promise<void>
  list(): Promise<{ alarms: Alarm[] }>
}

export const MawaeediAlarms = registerPlugin<MawaeediAlarmsPlugin>('MawaeediAlarms')

export async function scheduleMawaeediAlarm(alarm: Alarm) {
  return MawaeediAlarms.schedule(alarm)
}

export async function cancelMawaeediAlarm(id: string) {
  return MawaeediAlarms.cancel({ id })
}

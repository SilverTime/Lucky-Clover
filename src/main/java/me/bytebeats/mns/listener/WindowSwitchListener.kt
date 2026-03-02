package me.bytebeats.mns.listener

import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

abstract class WindowSwitchListener : WindowAdapter() {
    abstract override fun windowOpened(e: WindowEvent?)
    abstract override fun windowClosed(e: WindowEvent?)
}
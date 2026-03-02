package me.bytebeats.mns.listener

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

abstract class MousePressedListener : MouseAdapter() {
    abstract override fun mousePressed(e: MouseEvent)
}
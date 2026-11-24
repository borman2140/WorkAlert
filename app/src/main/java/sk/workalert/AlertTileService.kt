package sk.workalert

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class AlertTileService : TileService() {
    override fun onClick() {
        super.onClick()
        AlertTone.play(this)
        qsTile?.state = Tile.STATE_INACTIVE
        qsTile?.updateTile()
    }

    override fun onStartListening() {
        super.onStartListening()
        qsTile?.state = Tile.STATE_INACTIVE
        qsTile?.label = "WorkAlert"
        qsTile?.updateTile()
    }
}

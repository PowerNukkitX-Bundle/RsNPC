package com.smallaswater.npc.form;

import cn.nukkit.Player;
import cn.nukkit.Server;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.EventPriority;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerServerSettingsRequestEvent;
import cn.nukkit.event.server.DataPacketReceiveEvent;
import cn.nukkit.form.window.Form;
import cn.nukkit.network.protocol.NPCRequestPacket;
import cn.nukkit.network.protocol.ServerSettingsResponsePacket;
import com.smallaswater.npc.RsNPC;
import com.smallaswater.npc.form.windows.AdvancedFormWindowDialog;

import java.util.HashMap;
import java.util.Map;

/**
 * 窗口操作监听器
 * 实现AdvancedFormWindow AdvancedInventory操作处理
 *
 * @author LT_Name
 */
@SuppressWarnings("unused")
public class WindowListener implements Listener {

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDataPacketReceive(DataPacketReceiveEvent event) {
        if (event.getPacket() instanceof NPCRequestPacket) {
            if (AdvancedFormWindowDialog.onEvent((NPCRequestPacket) event.getPacket(), event.getPlayer())) {
                event.setCancelled(true);
            }
        }
    }

//    @EventHandler(priority = EventPriority.MONITOR)
//    public void onPlayerFormResponded(PlayerFormRespondedEvent event) {
//        if (SimpleForm.onEvent(event.getWindow(), event.getPlayer())) {
//            return;
//        }
//        if (ModalForm.onEvent(event.getWindow(), event.getPlayer())) {
//            return;
//        }
//        AdvancedFormWindowCustom.onEvent(event.getWindow(), event.getPlayer());
//    }

//    @EventHandler(priority = EventPriority.MONITOR)
//    public void onPlayerSettingsResponded(PlayerSettingsRespondedEvent event) {
//        AdvancedFormWindowCustom.onEvent(event.getWindow(), event.getPlayer());
//    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onSettingsRequest(PlayerServerSettingsRequestEvent event) {
        Player player = event.getPlayer();
        HashMap<Integer, Form<?>> map = new HashMap<>(event.getSettings());
        event.setSettings(new HashMap<>());
        //必须延迟一下，否则客户端不显示
        Server.getInstance().getScheduler().scheduleDelayedTask(RsNPC.getInstance(), () -> {
            for (Map.Entry<Integer, Form<?>> entry : map.entrySet()) {
                ServerSettingsResponsePacket pk = new ServerSettingsResponsePacket();
                pk.formId = entry.getKey();
                pk.data = entry.getValue().toJson();
                player.dataPacket(pk);
            }
        }, 20);
    }
}

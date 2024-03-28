package net.crossager.tacticalexample;

import net.crossager.tactical.api.commands.TacticalCommand;
import net.crossager.tactical.api.commands.argument.TacticalCommandArgument;
import net.crossager.tactical.api.commands.argument.TacticalCommandArgumentPrecondition;
import net.crossager.tactical.api.gui.animations.TacticalAnimationStyle;
import net.crossager.tactical.api.gui.animations.TacticalAnimator;
import net.crossager.tactical.api.gui.input.TacticalAnvilInputGUI;
import net.crossager.tactical.api.gui.input.TacticalSignGUI;
import net.crossager.tactical.api.gui.inventory.ItemUtils;
import net.crossager.tactical.api.gui.inventory.TacticalInventoryGUI;
import net.crossager.tactical.api.gui.inventory.components.TacticalGUIContainer;
import net.crossager.tactical.api.gui.inventory.components.TacticalStaticGUIComponent;
import net.crossager.tactical.api.gui.inventory.components.TacticalStorageGUIComponent;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class TacticalExample extends JavaPlugin {
    @Override
    public void onEnable() {
        TacticalCommand.create(this, "opensigngui")
                .addArgument(TacticalCommandArgument.string("line").addPrecondition(TacticalCommandArgumentPrecondition.stringNotLongerThan(15)).required(false))
                .commandExecutor(context -> {
                    TacticalSignGUI.create(context.argument("line", "").asString(), "^^^^^^", "Enter number")
                            .color(DyeColor.BLUE)
                            .glowing(true)
                            .onClose((player, input) -> {
                                player.sendMessage("You wrote line: " + input.line(0));
                            })
                            .open(context.playerSender());
                })
                .register();

        List<String> bannedWords = List.of("fuck", "shit");
        TacticalCommand.create(this, "openanvilgui")
                .addArgument(TacticalCommandArgument.string("title").required(false))
                .commandExecutor(context -> {
                    TacticalAnvilInputGUI.create()
                            .hideInventoryItems(true)
                            .title(ChatColor.GOLD + context.argument("title", "cool title").asString())
                            .itemNameModifier((itemStack, name) -> {
                                if (name.isEmpty()) return;
                                if (bannedWords.contains(name))
                                    ItemUtils.setName(itemStack, ChatColor.RED + "Swear words are not allowed");
                                else
                                    ItemUtils.setName(itemStack, ChatColor.ITALIC + name);
                            })
                            .renamingValidator(name -> !bannedWords.contains(name))
                            .onClose((player, input) -> {
                                player.sendMessage("You wrote line: " + input.renamedText());
                            })
                            .onRenaming((player, renamedText) -> {
                                player.sendMessage("Renaming: " + renamedText);
                            })
                            .onCancel((player, input) -> {
                                player.sendMessage("Filthy, you cancelled");
                            })
                            .open(context.playerSender());
                })
                .register();
        TacticalInventoryGUI gui = TacticalInventoryGUI.create(5, "§4Barrier maker");

        gui.addAnimationArea(1, 3, 0, 3, 0);
        TacticalStorageGUIComponent barrierMaker = TacticalStorageGUIComponent.create(3, 3);
        TacticalStorageGUIComponent storage = TacticalStorageGUIComponent.create(3, 3);
        TacticalGUIContainer barrierGUI = TacticalGUIContainer.create(3, 3).setComponent(0, 0, barrierMaker);
        TacticalStorageGUIComponent barrierInput = TacticalStorageGUIComponent.create(1, 1)
                .onItemsUpdate((player, items) -> {
                    Bukkit.getScheduler().runTask(this, () -> gui.updateDisplay(player));
                });
        TacticalStaticGUIComponent barrierOutput = TacticalStaticGUIComponent.of(player -> {
                    List<ItemStack> items = barrierInput.items(player);
                    if (items.get(0).getType() == Material.AIR) return ItemUtils.AIR;
                    return new ItemStack(Material.BARRIER, items.get(0).getAmount());
                })
                .onClick(event -> {
                    if (event.player().getItemOnCursor().getType() != Material.AIR) return;

                    List<ItemStack> items = barrierInput.items(event.player());
                    if (items.get(0).getType() == Material.AIR) return;
                    event.player().setItemOnCursor(new ItemStack(Material.BARRIER, items.get(0).getAmount()));
                    barrierInput.clear(event.player());
                    event.gui().updateDisplay(event.player());
                });
        TacticalGUIContainer barrierCrafter = TacticalGUIContainer.create(3, 3)
                .fillComponent(0, 0, 2, 2, TacticalStaticGUIComponent.of(ItemUtils.setName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ")))
                .setComponent(0, 1, barrierInput)
                .setComponent(2, 1, barrierOutput);
        Map<Player, Boolean> isPlayerOnPage = new HashMap<>();
        gui
                .createBorder(TacticalStaticGUIComponent.of(ItemUtils.setName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ")))
                .canPlayerInteractWithInventory(true)
                .setComponent(0, 0, TacticalStaticGUIComponent.of(new ItemStack(Material.BARRIER)).onClick(event -> {
                    if (event.player().getItemOnCursor().getType() != Material.AIR) {
                        event.player().setItemOnCursor(new ItemStack(Material.BARRIER));
                        event.player().sendMessage("Barrier");
                    }
                }))
                .setComponent(1, 0, TacticalStaticGUIComponent.of(new ItemStack(Material.DIAMOND_AXE)).onClick(event -> {
                    System.out.println(barrierMaker.items(event.player()).get(0));
                }))
                .setComponent(2, 0, TacticalStaticGUIComponent.of(new ItemStack(Material.GHAST_TEAR)).onClick(event -> {
                    if (!isPlayerOnPage.getOrDefault(event.player(), false)) {
                        barrierGUI.setComponent(0, 0, barrierCrafter);
                        isPlayerOnPage.put(event.player(), true);
                    } else {
                        barrierGUI.setComponent(0, 0, barrierMaker);
                        isPlayerOnPage.put(event.player(), false);
                    }
                    event.gui().updateDisplay(event.player());
                }))
                .setComponent(3, 0, TacticalStaticGUIComponent.of(new ItemStack(Material.LEATHER_HELMET))
                        .animate(context -> {
                            Color red = Color.RED;
                            Color yellow = Color.YELLOW;
                            int r = (int) ((red.getRed() * context.progress()) + (yellow.getRed() * (1 - context.progress())));
                            int g = (int) ((red.getGreen() * context.progress()) + (yellow.getGreen() * (1 - context.progress())));
                            int b = (int) ((red.getBlue() * context.progress()) + (yellow.getBlue() * (1 - context.progress())));
                            ItemStack itemStack = new ItemStack(Material.LEATHER_HELMET);
                            LeatherArmorMeta meta = (LeatherArmorMeta) itemStack.getItemMeta();
                            meta.setColor(Color.fromRGB(r, g, b));
                            itemStack.setItemMeta(meta);
                            return itemStack;
                        }, TacticalAnimator.of(TacticalAnimationStyle.SINE, 4, 0.1F, 0.1F)))
                .setComponent(1, 1, barrierGUI)
                .fillComponent(4, 0, 4, 4, TacticalStaticGUIComponent.of(ItemUtils.setName(new ItemStack(Material.BLACK_STAINED_GLASS_PANE), " ")))
                .setComponent(5, 1, storage)
                .onClose(player -> {
                    int total = 0;
                    for (ItemStack item : barrierMaker.items(player)) {
                        if (item.getType() == Material.AIR) continue;
                        player.getInventory().addItem(new ItemStack(Material.BARRIER, item.getAmount()));
                        total += item.getAmount();
                    }
                    barrierMaker.clear(player);
                    player.sendMessage("got %s barriers".formatted(total));
                });

        TacticalCommand.create(this, "gui")
                .commandExecutor(context -> {
                    gui.open(context.playerSender());
                })
                .register();
    }

    @Override
    public void onDisable() {

    }
}

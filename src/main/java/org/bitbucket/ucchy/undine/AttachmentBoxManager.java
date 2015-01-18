/*
 * @author     ucchy
 * @license    LGPLv3
 * @copyright  Copyright ucchy 2015
 */
package org.bitbucket.ucchy.undine;

import java.util.ArrayList;
import java.util.HashMap;

import org.bitbucket.ucchy.undine.sender.MailSender;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

/**
 * 添付ボックス管理クラス
 * @author ucchy
 */
public class AttachmentBoxManager {

    private static final String BOX_INV_META_NAME = "undine_boxinv";

    private Undine parent;

    private HashMap<Player, Inventory> editmodeBoxes;
    private HashMap<Integer, Inventory> attachmentBoxes;
    private HashMap<Player, Integer> indexCache;

    /**
     * コンストラクタ
     * @param parent プラグイン
     */
    public AttachmentBoxManager(Undine parent) {
        this.parent = parent;
        editmodeBoxes = new HashMap<Player, Inventory>();
        attachmentBoxes = new HashMap<Integer, Inventory>();
        indexCache = new HashMap<Player, Integer>();
    }

    /**
     * 指定されたプレイヤーに、そのプレイヤーの編集中ボックスを表示する
     * @param player プレイヤー
     * @param インベントリ名
     */
    protected String displayEditmodeBox(Player player) {

        // 既に、該当プレイヤーの編集中ボックスインベントリがある場合は、そちらを表示する
        if ( editmodeBoxes.containsKey(player) ) {
            player.openInventory(editmodeBoxes.get(player));
            return editmodeBoxes.get(player).getName();
        }

        // 添付ボックスの作成
        int size = parent.getUndineConfig().getAttachBoxSize() * 9;
        String title = Messages.get("EditmodeBoxTitle");

        // インベントリタイトルに32文字以上は設定できないので、必要に応じて削る
        if ( title.length() > 32 ) {
            title = title.substring(0, 32);
        }

        Inventory box = Bukkit.createInventory(player, size, title);

        // アイテムの追加
        MailSender sender = MailSender.getMailSender(player);
        MailData mail = parent.getMailManager().getEditmodeMail(sender);
        for ( ItemStack item : mail.getAttachments() ) {
            box.addItem(item);
        }

        editmodeBoxes.put(player, box);
        player.openInventory(box);
        return box.getName();
    }

    /**
     * 指定されたプレイヤーの編集中ボックスを取得する
     * @param player プレイヤー
     * @return 編集中ボックス
     */
    protected Inventory getEditmodeBox(Player player) {

        if ( editmodeBoxes.containsKey(player) ) {
            return editmodeBoxes.get(player);
        }
        return null;
    }

    /**
     * 該当プレイヤーの編集中ボックスをクリアする
     * @param player プレイヤー
     */
    protected void clearEditmodeBox(Player player) {

        if ( editmodeBoxes.containsKey(player) ) {
            editmodeBoxes.remove(player);
        }
    }

    /**
     * 指定されたメールの添付ボックスを開いて確認する
     * @param player 確認する人
     * @param mail メール
     * @param インベントリ名
     */
    protected String displayAttachmentBox(Player player, MailData mail) {

        // 既に、該当メールの添付ボックスインベントリがある場合は、そちらを表示する
        if ( attachmentBoxes.containsKey(mail.getIndex()) ) {
            player.openInventory(attachmentBoxes.get(mail.getIndex()));
            return attachmentBoxes.get(mail.getIndex()).getName();
        }

        // 添付ボックスの作成
        int size = (int)((mail.getAttachments().size() - 1) / 9 + 1) * 9;
        String title = Messages.get("AttachmentBoxTitle", "%number", mail.getIndex());

        // インベントリタイトルに32文字以上は設定できないので、必要に応じて削る
        if ( title.length() > 32 ) {
            title = title.substring(0, 32);
        }

        Inventory box = Bukkit.createInventory(player, size, title);

        // アイテムを追加
        for ( ItemStack item : mail.getAttachments() ) {
            box.addItem(item);
        }

        attachmentBoxes.put(mail.getIndex(), box);

        // 指定されたplayerの画面に添付ボックスを表示する
        player.openInventory(box);

        return box.getName();
    }

    /**
     * 指定されたメールの添付ボックスを開いて確認する
     * @param player 確認する人
     * @param mail メール
     */
    public void displayAttachBox(Player player, MailData mail) {

        String invname;
        if ( mail.isEditmode() ) {
            invname = parent.getBoxManager().displayEditmodeBox(player);
        } else {
            invname = parent.getBoxManager().displayAttachmentBox(player, mail);
        }

        // プレイヤーにメタデータを仕込む
        player.setMetadata(BOX_INV_META_NAME, new FixedMetadataValue(parent, invname));

        // メールのインデクスを記録しておく
        indexCache.put(player, mail.getIndex());
    }

    /**
     * 指定したプレイヤーが、添付ボックスを開いている状態かどうかを返す
     * @param player プレイヤー
     * @return 添付ボックスを開いているかどうか
     * （編集メールのボックスを含まない）
     */
    protected boolean isOpeningAttachBox(Player player) {
        return player.hasMetadata(BOX_INV_META_NAME)
                && indexCache.containsKey(player)
                && indexCache.get(player) > 0;
    }

    /**
     * 指定されたプレイヤーが開いていた添付ボックスを、メールと同期する
     * @param player プレイヤー
     */
    protected void syncAttachBox(Player player) {

        // メタデータが無いプレイヤーなら何もしない
        if ( !player.hasMetadata(BOX_INV_META_NAME) ) return;

        // 開いていたボックスのインデクスが記録されていないなら、何もしない
        if ( !indexCache.containsKey(player) ) return;

        // メタデータ、インデクスを削除する
        player.removeMetadata(AttachmentBoxManager.BOX_INV_META_NAME, parent);
        int index = indexCache.get(player);
        indexCache.remove(player);

        // 同期するボックスとメールを取得する
        MailData mail;
        Inventory inv;
        if ( index == 0 ) {
            mail = parent.getMailManager().getEditmodeMail(MailSender.getMailSender(player));
            inv = editmodeBoxes.get(player);
        } else {
            mail = parent.getMailManager().getMail(index);
            inv = attachmentBoxes.get(index);
        }

        // 一旦取り出して再度挿入することで、アイテムをスタックして整理する
        ArrayList<ItemStack> temp = new ArrayList<ItemStack>();
        for ( ItemStack item : inv.getContents() ) {
            if ( item != null && item.getType() != Material.AIR ) {
                temp.add(item);
            }
        }
        inv.clear();
        for ( ItemStack item : temp ) {
            inv.addItem(item);
        }

        ArrayList<ItemStack> array = new ArrayList<ItemStack>();
        for ( ItemStack item : inv.getContents() ) {
            if ( item != null && item.getType() != Material.AIR ) {
                array.add(item);
            }
        }

        // 同期して保存する
        mail.setAttachments(array);
        parent.getMailManager().saveMail(mail);
    }
}


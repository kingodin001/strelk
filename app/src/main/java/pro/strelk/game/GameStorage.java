package pro.strelk.game;

import android.content.Context;
import android.content.SharedPreferences;

public final class GameStorage {
    private final SharedPreferences pref;
    public GameStorage(Context c) { pref = c.getSharedPreferences("strelk_v1", Context.MODE_PRIVATE); }
    public int level() { return Math.max(1, Math.min(LevelFactory.TOTAL, pref.getInt("level",1))); }
    public int coins() { return Math.max(0, pref.getInt("coins",0)); }
    public int skin() { return pref.getInt("skin",0); }
    public boolean ownsSkin(int index) { return index == 0 || (pref.getInt("unlocked", 1) & (1 << index)) != 0; }
    public boolean buySkin(int index, int price) {
        if(index < 1 || index > 3 || ownsSkin(index) || !spend(price)) return false;
        pref.edit().putInt("unlocked",pref.getInt("unlocked",1) | (1 << index)).putInt("skin",index).apply();
        return true;
    }
    public int wins() { return Math.max(0, pref.getInt("wins",0)); }
    public boolean sound() { return pref.getBoolean("sound",true); }
    public void sound(boolean enable) { pref.edit().putBoolean("sound",enable).apply(); }
    public void earn(int amount) { pref.edit().putInt("coins",coins()+Math.max(0,amount)).apply(); }
    public boolean spend(int amount) {
        if (amount < 0 || coins() < amount) return false;
        pref.edit().putInt("coins",coins()-amount).apply(); return true;
    }
    public void selectSkin(int skin) { pref.edit().putInt("skin",skin).apply(); }
    public void win(int level) {
        if (level >= level() && level < LevelFactory.TOTAL) pref.edit().putInt("level",level+1).apply();
        pref.edit().putInt("wins",wins()+1).apply();
        earn(20 + Math.min(80,level/15));
    }
}

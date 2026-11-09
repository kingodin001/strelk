package pro.strelk.game;

import android.app.Activity;
import android.os.Bundle;
import android.view.WindowManager;
import android.view.Window;

public class MainActivity extends Activity {
    private GameView game;
    private GameStorage storage;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(5894 | 1024 | 512);
        storage = new GameStorage(this);
        game = new GameView(this, storage);
        setContentView(game);
    }
    @Override public void onBackPressed() { if (!game.back()) super.onBackPressed(); }
    @Override protected void onPause() { super.onPause(); if (game!=null) game.setPaused(true); }
    @Override protected void onResume() { super.onResume(); if (game!=null) game.setPaused(false); }
}

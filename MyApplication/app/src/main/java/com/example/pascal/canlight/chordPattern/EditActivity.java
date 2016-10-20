package com.example.pascal.canlight.chordPattern;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.pascal.canlight.audioPlayer.GetTrackActivity;
import com.example.pascal.canlight.audioPlayer.Player;
import com.example.pascal.canlight.audioPlayer.PlayerView;
import com.example.pascal.canlight.audioPlayer.SpotifyPlayer;
import com.example.pascal.canlight.MainActivity;
import com.example.pascal.canlight.R;
import com.example.pascal.canlight.SliderDialog;
import com.example.pascal.canlight.Song;
import com.example.pascal.canlight.SpotifySpinner;
import com.example.pascal.canlight.audioPlayer.YouTubePlayer;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.Spotify;

/**
 * Created by pascal on 02.10.16.
 */
public class EditActivity extends AppCompatActivity {

    private Song mCurrentSong;
    private MenuItem mAutoScrollPlayPauseMenuItem;
    private Menu mOptionsMenu;
    private SpotifyPlayer mSpotifyPlayer;
    private YouTubePlayer mYouTubePlayer;
    private Player mActivePlayer;
    private boolean mShowPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        final boolean keepScreenOn = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("pref_keep_screen_on", false);
        if (keepScreenOn) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit);
        mCurrentSong = getIntent().getParcelableExtra("song");

        final ChordPatternEdit editText = (ChordPatternEdit) findViewById(R.id.editText);

        editText.setText(mCurrentSong.getPattern());
        editText.setTextSize((float) mCurrentSong.getPatternTextSize());
        editText.setFocusable(false);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(mCurrentSong.getName());

        if (mCurrentSong.getPattern() == null) {
            importPattern();
        }
        setScrollRate(mCurrentSong.getScrollRate());
        if (mCurrentSong.getPatternIsUninitialized()) {
            importPattern();
        }

        ((AutoScrollView) findViewById(R.id.autoScrollView))
                .setOnAutoScrollStoppedListener(new AutoScrollView.OnAutoScrollStoppedListener() {
                    @Override
                    public void onAutoScrollStopped() {
                        updateAutoScrollStartPauseMenuItem(false);
                    }
                });

        if (mCurrentSong.getTrackId() == null || mCurrentSong.getTrackId().isEmpty()) {
            initializeTrackId(mCurrentSong);
        }

        mSpotifyPlayer = new SpotifyPlayer(this);
        mYouTubePlayer = new YouTubePlayer(this);

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mYouTubePlayer.pause();
        mSpotifyPlayer.pause();
        mYouTubePlayer.deinit();
        mSpotifyPlayer.deinit();
    }

    public boolean onOptionsItemSelected(MenuItem item){
        returnToMain();
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.edit_menu, menu);
        mAutoScrollPlayPauseMenuItem = menu.findItem(R.id.menu_auto_scroll_start_pause);
        mOptionsMenu = menu;

        // Edit Stuff
        menu.findItem(R.id.menu_transpose_up).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ChordPatternEdit cpe = (ChordPatternEdit) findViewById(R.id.editText);
                cpe.transpose(1);
                return true;
            }
        });
        menu.findItem(R.id.menu_transpose_down).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ChordPatternEdit cpe = (ChordPatternEdit) findViewById(R.id.editText);
                cpe.transpose(-1);
                return true;
            }
        });
        menu.findItem(R.id.menu_import_pattern).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                importPattern();
                return true;
            }
        });
        menu.findItem(R.id.menu_eliminate_empty_lines).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ChordPatternEdit cpe = (ChordPatternEdit) findViewById(R.id.editText);
                final int n = cpe.eliminateEmptyLines();
                final String text = getResources().getQuantityString(R.plurals.numberOfRemovedLines, n, n);
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                return true;
            }
        });
        menu.findItem(R.id.menu_add_empty_lines_before_chords).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                ChordPatternEdit cpe = (ChordPatternEdit) findViewById(R.id.editText);
                final int n = cpe.addEmptyLinesBeforeChords();
                final String text = getResources().getQuantityString(R.plurals.numberOfInsertedLines, n, n);
                Toast.makeText(getApplicationContext(), text, Toast.LENGTH_SHORT).show();
                return true;
            }
        });


        // Switch Edit/view stuff
        menu.findItem(R.id.menu_edit_pattern).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                setReadOnly(false);
                return true;
            }
        });
        menu.findItem(R.id.menu_view_pattern).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                setReadOnly(true);
                mCurrentSong.setPattern(((EditText) findViewById(R.id.editText)).getText().toString());
                return true;
            }
        });

        menu.findItem(R.id.menu_cancel_edit).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                setReadOnly(true);
                ((EditText) findViewById(R.id.editText)).setText(mCurrentSong.getPattern());
                return true;
            }
        });

        // View stuff
        menu.findItem(R.id.menu_auto_scroll_start_pause).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                AutoScrollView autoScroller = (AutoScrollView) findViewById(R.id.autoScrollView);
                if (autoScroller.isActive()) {
                    autoScroller.endAutoScroll();
                } else {
                    autoScroller.startAutoScroll();
                }
                updateAutoScrollStartPauseMenuItem(autoScroller.isActive());
                return true;
            }
        });
        menu.findItem(R.id.menu_auto_scroll_speed).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                new SliderDialog.ExpSliderDialog(0, 6, 1, EditActivity.this) {
                    @Override
                    protected void onValueChanged(double value) {
                        setScrollRate(value);
                    }
                }.setValue(mCurrentSong.getScrollRate());
                return true;
            }
        });
        menu.findItem(R.id.menu_scale_pattern).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                new SliderDialog.ExpSliderDialog(2, 30, 3, EditActivity.this) {
                    @Override
                    protected void onValueChanged(double value) {
                        setTextSize(value);
                    }
                }.setValue(mCurrentSong.getPatternTextSize());
                return true;
            }
        });
        menu.findItem(R.id.menu_toogle_player_visibility).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                setPlayerVisibility(findViewById(R.id.player).getVisibility() != View.VISIBLE);
                return true;
            }
        });
        menu.findItem(R.id.menu_config_player).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(EditActivity.this, GetTrackActivity.class);
                intent.putExtra("label", mCurrentSong.getTrackLabel());
                intent.putExtra("service", "youtube"); //TODO
                EditActivity.this.startActivityForResult(intent, MainActivity.GET_TRACK_REQUEST);

                return true;
            }
        });

        setReadOnly(true);
        setPlayerVisibility(mShowPlayer);
        return true;
    }

    private void setTrack(String service, String id, String label, long pos) {
        mCurrentSong.setTrack(service, id, label);
        if (mActivePlayer != null) {
            mActivePlayer.pause();
        }
        if (GetTrackActivity.SERVICES[0].equals(service)) {
            mActivePlayer = mSpotifyPlayer;
            mActivePlayer.init(id, 0);
        } else {
            Toast.makeText(this, "No Youtube yet.", Toast.LENGTH_LONG).show();
        }

        if (mActivePlayer != null) {
            ((PlayerView) findViewById(R.id.player)).setPlayer(mActivePlayer);
            mActivePlayer.seek(pos);
        }
    }

    private void setPlayerVisibility(boolean isVisible) {
        View player = findViewById(R.id.player);
        if (isVisible) {
            mShowPlayer = true;
            player.setVisibility(View.VISIBLE);
            setTrack(mCurrentSong.getTrackService(),
                    mCurrentSong.getTrackId(),
                    mCurrentSong.getTrackLabel(),
                    0);
        } else {
            mShowPlayer = false;
            player.setVisibility(View.GONE);
            if (mActivePlayer != null) {
                mActivePlayer.pause();
            }
        }
    }

    private void setTextSize(double s) {
        ChordPatternEdit cpe = (ChordPatternEdit) findViewById(R.id.editText);
        mCurrentSong.setPatternTextSize(s);
        cpe.setTextSize((float) s);
    }

    void updateAutoScrollStartPauseMenuItem(boolean play) {
        if (play) {
            mAutoScrollPlayPauseMenuItem.setTitle(R.string.menu_auto_scroll_pause_title);
            mAutoScrollPlayPauseMenuItem.setIcon(android.R.drawable.ic_media_pause);
        } else {
            mAutoScrollPlayPauseMenuItem.setTitle(R.string.menu_auto_scroll_start_title);
            mAutoScrollPlayPauseMenuItem.setIcon(android.R.drawable.ic_media_play);
        }
    }

    private void setScrollRate(double rate) {
        mCurrentSong.setScrollRate(rate);
        AutoScrollView autoScroller = (AutoScrollView) findViewById(R.id.autoScrollView);
        autoScroller.setScrollRate(rate);
    }

    public void setReadOnly(boolean ro) {
        AutoScrollView autoScroller = (AutoScrollView) findViewById(R.id.autoScrollView);
        mOptionsMenu.findItem(R.id.menu_edit_pattern).setVisible(ro);
        mOptionsMenu.findItem(R.id.menu_auto_scroll_start_pause).setVisible(ro);
        mOptionsMenu.findItem(R.id.menu_auto_scroll_speed).setVisible(ro);
        mOptionsMenu.findItem(R.id.menu_scale_pattern).setVisible(ro);
        mOptionsMenu.findItem(R.id.menu_toogle_player_visibility).setVisible(ro);
        mOptionsMenu.findItem(R.id.menu_view_pattern).setVisible(!ro);
        mOptionsMenu.findItem(R.id.menu_cancel_edit).setVisible(!ro);
        mOptionsMenu.findItem(R.id.menu_transpose_up).setVisible(!ro);
        mOptionsMenu.findItem(R.id.menu_transpose_down).setVisible(!ro);
        mOptionsMenu.findItem(R.id.menu_import_pattern).setVisible(!ro);
        mOptionsMenu.findItem(R.id.menu_eliminate_empty_lines).setVisible(!ro);
        mOptionsMenu.findItem(R.id.menu_add_empty_lines_before_chords).setVisible(!ro);

        final ChordPatternEdit editText = (ChordPatternEdit) findViewById(R.id.editText);
        editText.setIsEditable(!ro);
        autoScroller.endAutoScroll();
        if (ro) {
            editText.setTextSize((float) mCurrentSong.getPatternTextSize());
        } else {
            editText.setTextSize(18);
        }

        onPrepareOptionsMenu(mOptionsMenu);
    }

    private void importPattern() {
        Intent intent = new Intent(EditActivity.this, ImportPatternActivity.class);
        intent.putExtra("name", mCurrentSong.getName());
        EditActivity.this.startActivityForResult(intent, MainActivity.IMPORT_PATTERN_REQUEST);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case MainActivity.IMPORT_PATTERN_REQUEST:
                if (resultCode == RESULT_OK) {
                    final String pattern = data.getExtras().getString("pattern");
                    ChordPatternEdit cpe = (ChordPatternEdit) findViewById(R.id.editText);
                    cpe.setText(pattern);
                    mCurrentSong.setPattern(pattern);
                }
                break;
            case MainActivity.LOGIN_SPOTIFY_REQUEST:
                AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
                if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                    Config playerConfig = new Config(this, response.getAccessToken(), SpotifyPlayer.CLIENT_ID);
                    Spotify.getPlayer(playerConfig, this, new com.spotify.sdk.android.player.SpotifyPlayer.InitializationObserver() {
                        @Override
                        public void onInitialized(com.spotify.sdk.android.player.SpotifyPlayer spotifyPlayer) {
                            mSpotifyPlayer.onInitialized(spotifyPlayer);
                        }

                        @Override
                        public void onError(Throwable throwable) {
                            Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                        }
                    });
                }
                break;
            case MainActivity.GET_TRACK_REQUEST:
                if (resultCode == RESULT_OK) {
                    final String id = data.getStringExtra("id");
                    final String service = data.getStringExtra("service");
                    final String label = data.getStringExtra("label");
                    setTrack(service, id, label, 0);
                } else {
                    // ignore.
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        returnToMain();
    }

    private void returnToMain() {
        final EditText editText = (EditText) findViewById(R.id.editText);
        Intent resultIntent = new Intent();
        mCurrentSong.setPattern(editText.getText().toString());
        resultIntent.putExtra("song", mCurrentSong);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private void initializeTrackId(Song song) {
        SpotifySpinner.findTrack(song);
    }

}
package com.cdnbye.p2pengine;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cdnbye.sdk.P2pEngine;


import com.cdnbye.core.p2p.P2pStatisticsListener;
import com.cdnbye.core.p2p.PlayerInteractor;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.List;

public class PlayerActivity extends BaseActivity {

    private final String hls1 = "https://www.nmgxwhz.com:65/20200107/17hTnjxI/index.m3u8";
    private final String hls2 = "https://wowza.peer5.com/live/smil:bbb_abr.smil/chunklist_b591000.m3u8";
    private final String mp4_1 = "http://huya-w20.huya.com/2027/357649831/1300/e0a4cd303b58bab74f809be7f2d09113.mp4";
    private final String mp4_2 = "http://mirror.aarnet.edu.au/pub/TED-talks/911Mothers_2010W-480p.mp4";

    private PlayerView playerView;
    private SimpleExoPlayer player;

    private TextView offloadV;
    private TextView uploadV;
    private TextView peersV;
    private TextView connectedV;
    private TextView peerIdV;
    private TextView ratioV;
    private String currentUrl = hls1;

    private double totalHttpDownloaded = 0;
    private double totalP2pDownloaded = 0;
    private double totalP2pUploaded = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setNeedBackGesture(true);

        // bugly
//        CrashReport.initCrashReport(getApplicationContext(), "e40b652a35", true);

        setContentView(R.layout.activity_player);
        offloadV = findViewById(R.id.offload);
        uploadV = findViewById(R.id.upload);
        peersV = findViewById(R.id.peers);
        connectedV = findViewById(R.id.connected);
        peerIdV = findViewById(R.id.peerId);
        ratioV = findViewById(R.id.ratio);
        playerView = findViewById(R.id.player_view);
        TextView versionV = findViewById(R.id.version);
        versionV.setText("Version: " + P2pEngine.Version);

        // Recommended while playing living stream or mp4
        P2pEngine.getInstance().setPlayerInteractor(new PlayerInteractor() {
            @Override
            public long onBufferedDuration() {
                return player.getBufferedPosition() - player.getCurrentPosition();
            }
        });

        P2pEngine.getInstance().addP2pStatisticsListener(new P2pStatisticsListener() {
            @Override
            public void onHttpDownloaded(long value) {
                totalHttpDownloaded += (double) value;
                refreshRatio();

            }

            @Override
            public void onP2pDownloaded(long value, int speed) {
                totalP2pDownloaded += (double) value;
                String text = String.format("Offload: %.2fMB", totalP2pDownloaded / 1024);
                offloadV.setText(text);

                refreshRatio();
            }

            @Override
            public void onP2pUploaded(long value) {
                totalP2pUploaded += (double) value;
                String text = String.format("Upload: %.2fMB", totalP2pUploaded / 1024);
                uploadV.setText(text);
            }

            @Override
            public void onPeers(List<String> peers) {
                String text = String.format("Peers: %d", peers.size());
                peersV.setText(text);
            }

            @Override
            public void onServerConnected(boolean connected) {
                String text = String.format("Connected: %s", connected?"Yes":"No");
                connectedV.setText(text);
                String text2 = String.format("Peer ID: %s", P2pEngine.getInstance().getPeerId());
                peerIdV.setText(text2);
            }
        });

//        startPlay(currentUrl);

        Button hls1Btn = findViewById(R.id.hls1);
        Button hls2Btn = findViewById(R.id.hls2);
        Button mp4_1Btn = findViewById(R.id.mp4_1);
        Button mp4_2Btn = findViewById(R.id.mp4_2);

        hls1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearData();
                currentUrl = hls1;
                startPlay(currentUrl);
            }
        });

        hls2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearData();
                currentUrl = hls2;
                startPlay(currentUrl);
            }
        });
        mp4_1Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 清空数据
                clearData();
                currentUrl = mp4_1;
                startPlay(currentUrl);
            }
        });

        mp4_2Btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 清空数据
                clearData();
                currentUrl = mp4_2;
                startPlay(currentUrl);
            }
        });
    }

    private void startPlay(String url) {

        if (player != null && (player.isPlaying() || player.isLoading())) {
            player.stop(true);
            player.release();
        }
        // Convert original playback address (m3u8) to the address of the local proxy server
        String parsedUrl = P2pEngine.getInstance().parseStreamUrl(url);

        // Create a player instance.
        player = ExoPlayerFactory.newSimpleInstance(this);
        // Attach player to the view.
        playerView.setPlayer(player);
        // Prepare the player with media source.
        MediaSource mediaSource = buildMediaSource(Uri.parse(parsedUrl));

        player.prepare(mediaSource);
        // Start play when ready
        player.setPlayWhenReady(true);

    }

    private MediaSource buildMediaSource(Uri uri) {
        if (uri.getPath().endsWith(".m3u8")) {
            // Create a data source factory.
            DataSource.Factory dataSourceFactory =
                    new DefaultHttpDataSourceFactory(
                            Util.getUserAgent(this, "p2p-engine"),
                            DefaultHttpDataSource.DEFAULT_CONNECT_TIMEOUT_MILLIS,
                            DefaultHttpDataSource.DEFAULT_READ_TIMEOUT_MILLIS,
                            true   /* allowCrossProtocolRedirects */
                    );
            // Create a HLS media source pointing to a playlist uri.
            return new HlsMediaSource.Factory(dataSourceFactory).createMediaSource(uri);
        }
        return new ExtractorMediaSource.Factory(
                new DefaultHttpDataSourceFactory("p2p-engine")).
                createMediaSource(uri);
    }

    private void refreshRatio() {
        double ratio = 0;
        if (totalHttpDownloaded + totalP2pDownloaded != 0) {
            ratio = totalP2pDownloaded / (totalHttpDownloaded + totalP2pDownloaded);
        }
        String text = String.format("P2P Ratio: %.0f%%", ratio * 100);
        ratioV.setText(text);
    }

    private void clearData() {
        totalHttpDownloaded = 0;
        totalP2pDownloaded = 0;
        totalP2pUploaded = 0;
        refreshRatio();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}
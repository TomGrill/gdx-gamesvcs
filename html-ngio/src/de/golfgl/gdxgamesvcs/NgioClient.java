package de.golfgl.gdxgamesvcs;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

/**
 * Newgrounds.io client.
 * <p>
 * See newgrounds.io and newgrounds.com
 * <p>
 * Please note: Although this file would technically work in core module, it is placed in html project because it does
 * not work on other platforms than GWT. NGIO blocks direct calls to the API from other clients than webbrowsers.
 * <p>
 * Created by Benjamin Schulte on 17.06.2017.
 */

public class NgioClient implements IGameServiceClient {

    public static final String GAMESERVICE_ID = "GS_NGIO";
    public static final String NGIO_GATEWAY = "https://www.newgrounds.io/gateway_v3.php";

    protected IGameServiceListener gsListener;
    protected String ngAppId;
    protected String sessionId;
    protected boolean initialized;
    private String ngEncryptionKey;
    private boolean connected;
    private boolean connectionPending;
    private String userName;
    private int userId;

    @Override
    public String getGameServiceId() {
        return GAMESERVICE_ID;
    }

    @Override
    public void setListener(IGameServiceListener gsListener) {
        this.gsListener = gsListener;
    }

    public void initialize(String ngAppId, String ngioSessionid, String ngEncryptionKey) {
        this.ngAppId = ngAppId;
        this.sessionId = ngioSessionid;
        this.ngEncryptionKey = ngEncryptionKey;
        this.initialized = true;
    }

    @Override
    public void connect(final boolean silent) {
        if (connected)
            return;

        //TODO: Ping every 5 minutes

        if (!initialized) {
            Gdx.app.error(GAMESERVICE_ID, "Cannot connect before initizalize is called.");
            return;
        }

        if (sessionId == null && !silent && gsListener != null)
            gsListener.gsErrorMsg("Please sign in to Newgrounds and reload the game to use your Newgrounds account.");

        if (sessionId != null) {
            connectionPending = true;

            // yeah, I know I could do that better... but hey, at least it is fast!
            sendToGateway("{\"app_id\": \"" + ngAppId + "\",\"session_id\":\"" + sessionId + "\","
                            + "\"call\": {\"component\": " +
                            "\"App.checkSession\",\"parameters\": {}}}\n",
                    new RequestResultRunnable() {
                        @Override
                        public void run(String json) {
                            connectionPending = false;
                            checkSessionAnswer(silent, json);
                        }
                    });
        }

    }

    protected void checkSessionAnswer(boolean silent, String json) {
        JsonValue root = new JsonReader().parse(json);
        boolean success = root.getBoolean("success", false);

        String errorMsg = "";
        JsonValue errorObject = null;

        if (success) {

            try {
                JsonValue resultObjectData = root.get("result").get("data");

                connected = resultObjectData.getBoolean("success");

// => Answer when session is invalid:
// {"success":true,"app_id":"46188:ARRyvuAv","result":{"component":"App.checkSession","data":{"success":false,
// "error":{"message":"Session, with id \"aa9f2716754951641744e7628247f54237cb2b8d8bed24\", is invalid or expired.",
// "code":104},"parameters":{}}}}

//=> Answer when session is valid:
// {"success":true,"app_id":"46188:ARRyvuAv","result":{"component":"App.checkSession","data":{"success":true,
// "session":{"id":"aa9f2716754951641744e7628247f54237cb2b8d8bed24","user":{"id":6406923,"name":"MrStahlfelge",
// "icons":{"small":"http:\/\/img.ngfiles.com\/defaults\/icon-user-smallest.gif","medium":"http:\/\/img.ngfiles
// .com\/defaults\/icon-user-smaller.gif","large":"http:\/\/img.ngfiles.com\/defaults\/icon-user.gif"},
// "url":"http:\/\/mrstahlfelge.newgrounds.com","supporter":false},"expired":false,"remember":false}}}}

                if (connected) {
                    JsonValue userData = resultObjectData.get("session").get("user");
                    userName = userData.getString("name");
                    userId = userData.getInt("id");
                } else {
                    errorMsg = "User session invalid";
                    errorObject = resultObjectData.get("error");
                }

            } catch (Throwable t) {
                Gdx.app.error(GAMESERVICE_ID, "Error checking session - could not parse user data");
            }

            gsListener.gsConnected();
        } else {
//=> Answer when call is blocked:
// {"success":false,"error":{"message":"You have been making too many calls to the API and have been temporarily
// blocked. Try again in 96 seconds.","code":107},"api_version":"3.0.0","help_url":"http:\/\/www.newgrounds
// .com\/wiki\/creator-resources\/newgrounds-apis\/newgrounds-io"}

            connected = false;

            errorObject = root.get("error");
            errorMsg = "Error checking session";

        }

        if (!connected) {
            if (errorObject != null)
                errorMsg = errorMsg + ": " + errorObject.getInt("code") + "/" + errorObject.getString("message");

            Gdx.app.log(GAMESERVICE_ID, errorMsg);

            if (!silent && gsListener != null)
                gsListener.gsErrorMsg(errorMsg);
        }

        if (gsListener != null) {
            if (connected)
                // yeah!
                gsListener.gsConnected();
            else
                // reset pending state
                gsListener.gsDisconnected();
        }
    }

    @Override
    public void disconnect() {
        //TODO: Deactivate ping
        connected = false;

        if (gsListener != null)
            gsListener.gsDisconnected();
    }

    @Override
    public void logOff() {
        // changing this value leads to not making any calls to the gateway anymore
        userName = null;
        userId = 0;

        disconnect();
    }

    @Override
    public String getPlayerDisplayName() {
        return userName;
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isConnectionPending() {
        return connectionPending && !connected;
    }

    @Override
    public boolean providesLeaderboardUI() {
        return false;
    }

    @Override
    public void showLeaderboards(String leaderBoardId) throws GameServiceException {
        throw new UnsupportedOperationException(GAMESERVICE_ID);
    }

    @Override
    public boolean providesAchievementsUI() {
        return false;
    }

    @Override
    public void showAchievements() throws GameServiceException {
        throw new UnsupportedOperationException(GAMESERVICE_ID);
    }

    @Override
    public void submitToLeaderboard(String leaderboardId, long score, String tag) throws GameServiceException {

    }

    @Override
    public void submitEvent(String eventId, int increment) {

    }

    @Override
    public void unlockAchievement(String achievementId) {

    }

    @Override
    public void incrementAchievement(String achievementId, int incNum) {
        throw new UnsupportedOperationException(GAMESERVICE_ID);
    }

    @Override
    public void saveGameState(byte[] gameState, long progressValue) {
        throw new UnsupportedOperationException(GAMESERVICE_ID);
    }

    @Override
    public void loadGameState() {
        throw new UnsupportedOperationException(GAMESERVICE_ID);
    }

    protected void sendToGateway(String content, RequestResultRunnable req) {
        sendForm(content, req);
    }

    /**
     * For some reason, calls to NGIO must be form-encoded. So we use a native call to let the browser do the
     * encoding stuff. Copied from the newgrounds example.
     *
     * @param json
     */
    private native void sendForm(String json, NgioClient.RequestResultRunnable resultRun) /*-{
        var xhr = new XMLHttpRequest();

		xhr.onreadystatechange = function() {
			if (xhr.readyState==4) {
				resultRun.@de.golfgl.gdxgamesvcs.NgioClientOld.RequestResultRunnable::run(Ljava/lang/String;)(xhr
				.responseText);
			}
		};

		var formData = new FormData();
        formData.append('input', json);
		xhr.open('POST', '//www.newgrounds.io/gateway_v3.php', true);
		xhr.send(formData);

        }-*/;

    public static abstract class RequestResultRunnable {
        abstract public void run(String json);
    }
}
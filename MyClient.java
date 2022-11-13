import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MyClient extends JFrame implements MouseListener, MouseMotionListener {
    private final JButton[][] buttonArray;//ボタン用の配列
    private final Container c;
    int myNumberInt;
    private int myColor;
    private int myTurn;
    private ImageIcon myIcon, yourIcon;
    private final ImageIcon blackIcon;
    private final ImageIcon whiteIcon;
    private final ImageIcon boardIcon;
    private int x, y;
    private PrintWriter out;//出力用のライター


    public MyClient() {
        //名前の入力ダイアログを開く
        String myName = JOptionPane.showInputDialog(null, "名前を入力してください", "名前の入力", JOptionPane.QUESTION_MESSAGE);
        if(myName.equals("")) {
            myName = "No name";//名前がないときは，"No name"とする
        }

        //IPアドレス入力ダイアログ
        String myAddress = JOptionPane.showInputDialog(null, "IPアドレスを入力してください", "IPアドレスの入力", JOptionPane.QUESTION_MESSAGE);
        if(myAddress.equals("")) {
            myAddress = "localhost";//IPアドレスがないときは，"localhost"とする
        }

        //ウィンドウを作成する
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//ウィンドウを閉じるときに，正しく閉じるように設定する
        setTitle("MyClient");//ウィンドウのタイトルを設定する
        setSize(400, 430);//ウィンドウのサイズを設定する
        c = getContentPane();//フレームのペインを取得する

        //アイコンの設定
        whiteIcon = new ImageIcon("White.jpg");
        blackIcon = new ImageIcon("Black.jpg");
        boardIcon = new ImageIcon("GreenFrame.jpg");

        c.setLayout(null);//自動レイアウトの設定を行わない
        //ボタンの生成
        buttonArray = new JButton[8][8];//ボタンの配列を５個作成する[0]から[7]まで使える
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                buttonArray[j][i] = new JButton(boardIcon);//ボタンにアイコンを設定する
                c.add(buttonArray[j][i]);//ペインに貼り付ける
                buttonArray[j][i].setBounds(i * 45 + 10, j * 45 + 10, 45, 45);//ボタンの大きさと位置を設定する．(x座標，y座標,xの幅,yの幅）
                buttonArray[j][i].addMouseListener(this);//ボタンをマウスでさわったときに反応するようにする
                buttonArray[j][i].addMouseMotionListener(this);//ボタンをマウスで動かそうとしたときに反応するようにする
                buttonArray[j][i].setActionCommand(Integer.toString(j * 8 + i));//ボタンに配列の情報を付加する（ネットワークを介してオブジェクトを識別するため）
            }
            System.out.println();
        }
        //ボード初期化
        init();

        //サーバに接続する
        Socket socket = null;
        try {
            //"localhost"は，自分内部への接続．localhostを接続先のIP Address（"133.42.155.201"形式）に設定すると他のPCのサーバと通信できる
            //10000はポート番号．IP Addressで接続するPCを決めて，ポート番号でそのPC上動作するプログラムを特定する
            socket = new Socket(myAddress, 10000);
        }catch(UnknownHostException e) {
            System.err.println("ホストの IP アドレスが判定できません: " + e);
        }catch(IOException e) {
            System.err.println("エラーが発生しました: " + e);
        }

        MesgRecvThread mrt = new MesgRecvThread(socket, myName);//受信用のスレッドを作成する
        mrt.start();//スレッドを動かす（Runが動く）
    }

    //メッセージ受信のためのスレッド
    public class MesgRecvThread extends Thread {

        Socket socket;
        String myName;

        public MesgRecvThread(Socket s, String n) {
            socket = s;
            myName = n;
        }

        //通信状況を監視し，受信データによって動作する
        public void run() {
            try {
                InputStreamReader sisr = new InputStreamReader(socket.getInputStream());
                BufferedReader br = new BufferedReader(sisr);
                out = new PrintWriter(socket.getOutputStream(), true);
                out.println(myName);//接続の最初に名前を送る
                String myNumberStr = br.readLine();//コマの色と先手後手を設定
                myNumberInt = Integer.parseInt(myNumberStr);
                if(myNumberInt % 2 != 0) {
                    myColor = 0;
                    myIcon = blackIcon;
                    yourIcon = whiteIcon;
                    myTurn = 0;
                }else {
                    myColor = 1;
                    myIcon = whiteIcon;
                    yourIcon = blackIcon;
                    myTurn = 1;
                }
                label:
                while(true) {
                    String inputLine = br.readLine();//データを一行分だけ読み込んでみる
                    if(inputLine != null) {//読み込んだときにデータが読み込まれたかどうかをチェックする
                        System.out.println(inputLine);//デバッグ（動作確認用）にコンソールに出力する
                        String[] inputTokens = inputLine.split(" ");    //入力データを解析するために、スペースで切り分ける
                        String cmd = inputTokens[0];//コマンドの取り出し．１つ目の要素を取り出す
                        if(cmd.equals("changeTurn")) {//ターンの交換
                            myTurn = 1 - myTurn;
                            continue;
                        }
                        if(cmd.equals("fin")) {//相手からゲーム終了の知らせを受けて結果表示
                            judgeResult();//結果
                            break;
                        }
                        switch(cmd) {
                            case "move": {//cmdの文字と"move"が同じか調べる．同じ時にtrueとなる
                                //moveの時の処理(コマの移動の処理)
                                String theBName = inputTokens[1];//ボタンの名前（番号）の取得

                                int theBnumRaw = Integer.parseInt(theBName) / 8;//ボタンの名前を数値に変換する

                                int theBnumCol = Integer.parseInt(theBName) % 8;//ボタンの名前を数値に変換する

                                int x = Integer.parseInt(inputTokens[2]);//数値に変換する

                                int y = Integer.parseInt(inputTokens[3]);//数値に変換する

                                buttonArray[theBnumRaw][theBnumCol].setLocation(x, y);//指定のボタンを位置をx,yに設定する

                                break;
                            }
                            case "place": {//cmdの文字と"place"が同じか調べる．同じ時にtrueとなる
                                //placeの時の処理(コマを置く処理)
                                String theBName = inputTokens[1];//ボタンの名前（番号）の取得

                                int theBnumRaw = Integer.parseInt(theBName) / 8;//ボタンの名前を数値に変換する

                                int theBnumCol = Integer.parseInt(theBName) % 8;//ボタンの名前を数値に変換する

                                int theColor = Integer.parseInt(inputTokens[2]);//数値に変換する

                                if(theColor == myColor) {//アイコンがwhiteIconと同じなら(→変更後:myColorが0なら)
                                    buttonArray[theBnumRaw][theBnumCol].setIcon(myIcon);//blackIconに設定する

                                    //ゲームが続行可能か調べる
                                    changeIcon();//myIconとyourIconの入れ替え

                                    if(!judgeGame()) {//次に相手が打てるか調べる
                                        changeIcon();//アイコンをもとに戻す

                                        if(!judgeGame()) {//相手がパスとなる場合に自分が打てるか調べる
                                            judgeResult();//結果

                                            String msg = "fin";//相手にゲーム終了の通知

                                            //サーバに情報を送る
                                            out.println(msg);//送信データをバッファに書き出す

                                            out.flush();//送信データをフラッシュ（ネットワーク上にはき出す）する

                                            break label;
                                        }else {
                                            continue;
                                        }
                                    }else {
                                        changeIcon();//アイコンをもとに戻す

                                        String msg = "changeTurn";//ターン交換用のメッセージ作成

                                        //サーバに情報を送る
                                        out.println(msg);//送信データをバッファに書き出す

                                        out.flush();//送信データをフラッシュ（ネットワーク上にはき出す）する

                                    }
                                }else {
                                    buttonArray[theBnumRaw][theBnumCol].setIcon(yourIcon);//whiteIconに設定する

                                }
                                break;
                            }
                            case "flip": {//cmdの文字と"flip"が同じか調べる．同じ時にtrueとなる
                                //flipの時の処理(コマを置く処理)
                                String theBName = inputTokens[1];//ボタンの名前（番号）の取得

                                int theBnumRaw = Integer.parseInt(theBName) / 8;//ボタンの名前を数値に変換する

                                int theBnumCol = Integer.parseInt(theBName) % 8;//ボタンの名前を数値に変換する

                                int theColor = Integer.parseInt(inputTokens[2]);//数値に変換する

                                if(theColor == myColor) {//アイコンがwhiteIconと同じなら(→変更後:myColorが0なら)
                                    buttonArray[theBnumRaw][theBnumCol].setIcon(myIcon);//blackIconに設定する

                                }else {
                                    buttonArray[theBnumRaw][theBnumCol].setIcon(yourIcon);//whiteIconに設定する

                                }
                                break;
                            }
                            default:
                                break label;
                        }
                    }
                }
                socket.close();
            }catch(IOException e) {
                System.err.println("エラーが発生しました: " + e);
            }
        }
    }

    public static void main(String[] args) {
        MyClient net = new MyClient();
        net.setVisible(true);
    }

    public void mouseClicked(MouseEvent e) {//ボタンをクリックしたときの処理
        System.out.println("クリック");
        JButton theButton = (JButton) e.getComponent();//クリックしたオブジェクトを得る．型が違うのでキャストする
        String theArrayIndex = theButton.getActionCommand();//ボタンの配列の番号を取り出す
        int temp = Integer.parseInt(theArrayIndex);//座標に変換
        x = temp % 8;
        y = temp / 8;

        Icon theIcon = theButton.getIcon();//theIconには，現在のボタンに設定されたアイコンが入る
        System.out.println(theIcon);//デバッグ（確認用）に，クリックしたアイコンの名前を出力する

        if(myTurn == 0 && theIcon.equals(boardIcon)) {
            if(judgeButton(y, x)) {
                //送信情報を作成する（受信時には，この送った順番にデータを取り出す．スペースがデータの区切りとなる）
                String msg = "place" + " " + theArrayIndex + " " + myColor;

                //サーバに情報を送る
                out.println(msg);//送信データをバッファに書き出す
                out.flush();//送信データをフラッシュ（ネットワーク上にはき出す）する

                repaint();//画面のオブジェクトを描画し直す
            }else {
                System.out.println("そこには配置できません");
            }
        }
    }

    public void mouseEntered(MouseEvent e) {//マウスがオブジェクトに入ったときの処理
        //System.out.println("マウスが入った");
    }

    public void mouseExited(MouseEvent e) {//マウスがオブジェクトから出たときの処理
        //System.out.println("マウス脱出");
    }

    public void mousePressed(MouseEvent e) {//マウスでオブジェクトを押したときの処理（クリックとの違いに注意）
        //System.out.println("マウスを押した");
    }

    public void mouseReleased(MouseEvent e) {//マウスで押していたオブジェクトを離したときの処理
        //System.out.println("マウスを放した");
    }

    public void mouseDragged(MouseEvent e) {//マウスでオブジェクトとをドラッグしているときの処理
        /*
        System.out.println("マウスをドラッグ");
        JButton theButton = (JButton) e.getComponent();//型が違うのでキャストする
        String theArrayIndex = theButton.getActionCommand();//ボタンの配列の番号を取り出す

        if(Integer.parseInt(theArrayIndex) != 0) {
            Point theMLoc = e.getPoint();//発生元コンポーネントを基準とする相対座標
            System.out.println(theMLoc);//デバッグ（確認用）に，取得したマウスの位置をコンソールに出力する
            Point theBtnLocation = theButton.getLocation();//クリックしたボタンを座標を取得する
            theBtnLocation.x += theMLoc.x - 15;//ボタンの真ん中当たりにマウスカーソルがくるように補正する
            theBtnLocation.y += theMLoc.y - 15;//ボタンの真ん中当たりにマウスカーソルがくるように補正する
            theButton.setLocation(theBtnLocation);//マウスの位置にあわせてオブジェクトを移動する

            //送信情報を作成する（受信時には，この送った順番にデータを取り出す．スペースがデータの区切りとなる）
            String msg = "move" + " " + theArrayIndex + " " + theBtnLocation.x + " " + theBtnLocation.y;

            //サーバに情報を送る
            out.println(msg);//送信データをバッファに書き出す
            out.flush();//送信データをフラッシュ（ネットワーク上にはき出す）する

            repaint();//オブジェクトの再描画を行う
        }
         */
    }

    public void mouseMoved(MouseEvent e) {//マウスがオブジェクト上で移動したときの処理
        /*
        System.out.println("マウス移動");
        int theMLocX = e.getX();//マウスのx座標を得る
        int theMLocY = e.getY();//マウスのy座標を得る
        System.out.println(theMLocX + "," + theMLocY);//コンソールに出力する
         */
    }

    public void init() {//ボード初期化&ゲームリセット用
        for(int i = 0; i <= 7; i++) {
            for(int j = 0; j <= 7; j++) {
                buttonArray[i][j].setIcon(boardIcon);
            }
        }
        buttonArray[3][3].setIcon(whiteIcon);
        buttonArray[3][4].setIcon(blackIcon);
        buttonArray[4][3].setIcon(blackIcon);
        buttonArray[4][4].setIcon(whiteIcon);
    }

    public boolean judgeButton(int y, int x) {//コマが置けるかの判断
        boolean flag = false;

        for(int i = -1; i <= 1; i++) {
            for(int j = -1; j <= 1; j++) {
                if((y + j) >= 0 && (y + j) <= 7 && (x + i) >= 0 && (x + i) <= 7) {//場外判定
                    if(!(j == 0 && i == 0)) {//自分の位置以外を対象としているときに実行
                        int fNum = flipButtons(y, x, j, i);
                        if(fNum >= 1) {
                            for(int dy = j, dx = i, k = 0; k < fNum; k++, dy += j, dx += i) {//コマを置く前に、置いたらひっくり返る場所を処理
                                //ボタンの位置情報を作る
                                int msgy = y + dy;
                                int msgx = x + dx;
                                int theArrayIndex = msgy * 8 + msgx;

                                //サーバに情報を送る
                                String msg = "flip" + " " + theArrayIndex + " " + myColor;
                                out.println(msg);
                                out.flush();
                            }
                            flag = true;
                        }
                    }
                }
            }
        }
        return flag;
    }

    public int flipButtons(int y, int x, int j, int i) {//ひっくり返せる数をカウント
        int flipNum = 0;
        Icon icon;

        for(int dy = j, dx = i; ; dy += j, dx += i) {
            if((y + dy) >= 0 && (y + dy) <= 7 && (x + dx) >= 0 && (x + dx) <= 7) {
                icon = buttonArray[y + dy][x + dx].getIcon();
                if(icon == boardIcon) {
                    return 0;
                }else if(icon == myIcon) {
                    return flipNum;
                }else if(icon == yourIcon) {
                    flipNum++;
                }
            }else {
                return 0;
            }
        }
    }

    public boolean judgeGame() {//コマを置いて続行できるかの判断
        boolean flag = false;

        for(int b = 0; b <= 7; b++) {
            for(int a = 0; a <= 7; a++) {
                if(buttonArray[b][a].getIcon() == boardIcon) {//ボードアイコンとなっている場所について調べる
                    for(int s = -1; s <= 1; s++) {
                        for(int t = -1; t <= 1; t++) {
                            if((b + t) >= 0 && (b + t) <= 7 && (a + s) >= 0 && (a + s) <= 7) {
                                if(!(t == 0 && s == 0)) {
                                    int fNum = flipButtons(b, a, t, s);
                                    if(fNum >= 1) {//ひっくりかえせる場所を見つけたらゲームが続行可能と判断
                                        flag = true;
                                        return flag;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return flag;
    }

    public void judgeResult() {//ゲーム結果
        int blackNum = 0;
        int whiteNum = 0;
        for(int i = 0; i < 8; i++) {
            for(int j = 0; j < 8; j++) {
                Icon resultIcon = buttonArray[j][i].getIcon();
                if(resultIcon == blackIcon) {
                    blackNum++;
                }else {
                    whiteNum++;
                }
            }
        }
        if(blackNum == whiteNum) {
            System.out.println("Draw");
        }else if(blackNum > whiteNum) {
            System.out.println("Black Win");
        }else {
            System.out.println("White Win");
        }
    }

    public void changeIcon() {//コマのアイコンを交換
        ImageIcon changeIcon;
        changeIcon = myIcon;
        myIcon = yourIcon;
        yourIcon = changeIcon;
    }
}
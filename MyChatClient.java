import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

public class MyChatClient extends JFrame implements ActionListener {
    //チャット画面関連
    JTextField tfKeyin;//メッセージ入力用テキストフィールド
    JTextArea taMain;//テキストエリア
    String myName;//名前を保存
    JButton bs = new JButton("Send");
    private final Container c;
    PrintWriter out;//出力用のライター

    public MyChatClient() {
        //名前の入力ダイアログを開く
        String myName = JOptionPane.showInputDialog(null, "名前を入力してください", "名前の入力", JOptionPane.QUESTION_MESSAGE);
        if(myName == null) {
            myName = "No name";
        }

        //ウィンドウを作成する
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("MyChatClient");
        setSize(600, 400);
        c = getContentPane();
        c.setLayout(new FlowLayout());//レイアウトの設定

        //チャット画面を作成する
        tfKeyin = new JTextField("", 42);//入力用のテキストフィールドを作成
        c.add(tfKeyin);//コンテナに追加
        c.add(bs);//ボタンをコンテナに追加
        bs.addActionListener(this);//ボタンを押したときに動作するようにする
        taMain = new JTextArea(20, 50);//チャットの出力用のフィールドを作成
        c.add(taMain);//コンテナに追加
        taMain.setEditable(false);//編集不可にする

        //サーバに接続する
        Socket socket = null;
        try {
            //"localhost"は，自分内部への接続．localhostを接続先のIP Address（"133.42.155.201"形式）に設定すると他のPCのサーバと通信できる
            //10000はポート番号．IP Addressで接続するPCを決めて，ポート番号でそのPC上動作するプログラムを特定する
            socket = new Socket("localhost", 10000);
        }catch(UnknownHostException e) {
            System.err.println("ホストの IP アドレスが判定できません: " + e);
        }catch(IOException e) {
            System.err.println("エラーが発生しました: " + e);
        }

        MesgRecvThread mrt = new MesgRecvThread(socket, myName);
        mrt.start();
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
                while(true) {
                    String inputLine = br.readLine();
                    if(inputLine != null) {
                        taMain.append(inputLine + "\n");//メッセージの内容を出力用テキストに追加する
                    }else {
                        break;
                    }
                }
                socket.close();
            }catch(IOException e) {
                System.err.println("エラーが発生しました: " + e);
            }
        }
    }

    //アクションが行われたときの処理
    public void actionPerformed(ActionEvent ae) {
        if(ae.getActionCommand() == "Send") {
            String msg = tfKeyin.getText();//入力したテキストを得る
            tfKeyin.setText("");//tfKeyinのTextをクリアする
            if(msg.length() > 0) {
                out.println(msg);
                out.flush();
            }
        }
    }

    public static void main(String[] args) {
        MyChatClient cc = new MyChatClient();
        cc.setVisible(true);
    }
}

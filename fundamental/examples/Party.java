import java.awt.*;
import java.awt.event.*;


class Party {
    public void buildInvite() {
        Frame f = new Frame("Party Invitation");
        Label l = new Label("You are invited to a party!");
        Button b = new Button("Accept");

        f.setLayout(new FlowLayout());
        f.add(l);
        f.add(b);

        b.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("You accepted the invitation!");
            }
        });

        f.setSize(300, 100);
        f.setVisible(true);
    }
}
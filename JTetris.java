import java.awt.*;
import java.applet.*;
import java.awt.event.*;

class JCell
{
	int state;
	Color color;

	public JCell(int s, Color c)
	{
		state=s;
		color=c;
	}

	public void setCell(int s, Color c)
	{
		state=s;
		color=c;
	}
}

class JBrick
{
	Color color;
	int numcells;			// number of cells
	int numorients;			// number of orientations
	Dimension[][] parts;	// relative coordinates of parts
	Dimension startloc=new Dimension(4,1);		// coordinate of starting point

	public JBrick(int numc, int numo, Color c)
	{
		color=c;
		numcells=numc;
		numorients=numo;
		parts=new Dimension[numorients][numcells];
		for(int i=0;i<numorients;i++)
		{
			for(int j=0;j<numcells;j++)
			{
				parts[i][j]=new Dimension();
			}
		}
	}

	public void setStart(int x, int y)
	{
		startloc.setSize(x,y);	// override default
	}

	public void setPart(int orient, int cellno, int hinc, int vinc)
	{
		parts[orient][cellno].setSize(hinc,vinc);
	}

	public Dimension getPart(int orient, int cellno)
	{
		return parts[orient][cellno].getSize();
	}
}

class JTetrisCore extends Canvas implements Runnable
{
	Thread thread=null;

	final int NUMROWS=20;
	final int NUMCOLS=10;
	JCell[][] cell=new JCell[NUMCOLS][NUMROWS];
	Dimension cellsize=new Dimension();
	// cell states
	final int CELL_EMPTY=0;
	final int CELL_OCCUPIED=1;

	JBrick[] brick;
	int curbrick;
	int curorient;
	Dimension curloc=new Dimension();

	// off screen image related objects; for flicker prevention
	Image offimg;
	Dimension offsize;
	Graphics offg;
	
	int score;
	int delay=500;

	public JTetrisCore()
	{
		// Initialize bricks
		brick=new JBrick[7];

		//
		// [1][0][3]
		//    [2]
		brick[0]=new JBrick(4,4,Color.darkGray);
		brick[0].setPart(0,0,0,0);
		brick[0].setPart(0,1,-1,0);
		brick[0].setPart(0,2,0,1);
		brick[0].setPart(0,3,1,0);
		//    [1]
		//    [0][2]
		//    [3]
		brick[0].setPart(1,0,0,0);
		brick[0].setPart(1,1,0,-1);
		brick[0].setPart(1,2,1,0);
		brick[0].setPart(1,3,0,1);
		//    [2]
		// [1][0][3]
		//
		brick[0].setPart(2,0,0,0);
		brick[0].setPart(2,1,-1,0);
		brick[0].setPart(2,2,0,-1);
		brick[0].setPart(2,3,1,0);
		//    [1]
		// [3][0]
		//    [2]
		brick[0].setPart(3,0,0,0);
		brick[0].setPart(3,1,0,-1);
		brick[0].setPart(3,2,-1,0);
		brick[0].setPart(3,3,0,1);

		//
		//    [0][1]
		//    [3][2]
		brick[1]=new JBrick(4,1,Color.cyan);
		brick[1].setPart(0,0,0,0);
		brick[1].setPart(0,1,1,0);
		brick[1].setPart(0,2,1,1);
		brick[1].setPart(0,3,0,1);

		//
		// [3][0][1]
		// [2]
		brick[2]=new JBrick(4,4,Color.yellow);
		brick[2].setPart(0,0,0,0);
		brick[2].setPart(0,1,1,0);
		brick[2].setPart(0,2,-1,1);
		brick[2].setPart(0,3,-1,0);
		//    [1]
		//    [0]
		//    [3][2]
		brick[2].setPart(1,0,0,0);
		brick[2].setPart(1,1,0,-1);
		brick[2].setPart(1,2,1,1);
		brick[2].setPart(1,3,0,1);
		//       [3]
		// [2][0][1]
		//
		brick[2].setPart(2,0,0,0);
		brick[2].setPart(2,1,1,0);
		brick[2].setPart(2,2,-1,0);
		brick[2].setPart(2,3,1,-1);
		// [2][3]
		//    [0]
		//    [1]
		brick[2].setPart(3,0,0,0);
		brick[2].setPart(3,1,0,1);
		brick[2].setPart(3,2,-1,-1);
		brick[2].setPart(3,3,0,-1);

		//
		// [3][0][1]
		//       [2]
		brick[3]=new JBrick(4,4,Color.pink);
		brick[3].setStart(5,1);
		brick[3].setPart(0,0,0,0);
		brick[3].setPart(0,1,1,0);
		brick[3].setPart(0,2,1,1);
		brick[3].setPart(0,3,-1,0);
		//    [1][2]
		//    [0]
		//    [3]
		brick[3].setPart(1,0,0,0);
		brick[3].setPart(1,1,0,-1);
		brick[3].setPart(1,2,1,-1);
		brick[3].setPart(1,3,0,1);
		// [3]
		// [2][0][1]
		//
		brick[3].setPart(2,0,0,0);
		brick[3].setPart(2,1,1,0);
		brick[3].setPart(2,2,-1,0);
		brick[3].setPart(2,3,-1,-1);
		//    [3]
		//    [0]
		// [2][1]
		brick[3].setPart(3,0,0,0);
		brick[3].setPart(3,1,0,1);
		brick[3].setPart(3,2,-1,1);
		brick[3].setPart(3,3,0,-1);

		//
		// [2][3][0][1]
		//
		brick[4]=new JBrick(4,2,Color.red);
		brick[4].setStart(5,1);
		brick[4].setPart(0,0,0,0);
		brick[4].setPart(0,1,1,0);
		brick[4].setPart(0,2,-2,0);
		brick[4].setPart(0,3,-1,0);
		//       [2]
		//       [1]
		//       [0]
		//       [3]
		brick[4].setPart(1,0,0,0);
		brick[4].setPart(1,1,0,-1);
		brick[4].setPart(1,2,0,-2);
		brick[4].setPart(1,3,0,1);

		//  [3][0]
		//     [2][1]
		//
		brick[5]=new JBrick(4,2,Color.green);
		brick[5].setPart(0,0,0,0);
		brick[5].setPart(0,1,1,1);
		brick[5].setPart(0,2,0,1);
		brick[5].setPart(0,3,-1,0);
		//     [0]
		//  [2][1]
		//  [3]
		brick[5].setPart(1,0,0,0);
		brick[5].setPart(1,1,0,1);
		brick[5].setPart(1,2,-1,1);
		brick[5].setPart(1,3,-1,2);

		//     [0][1]
		//  [3][2]
		//
		brick[6]=new JBrick(4,2,Color.orange);
		brick[6].setStart(5,1);
		brick[6].setPart(0,0,0,0);
		brick[6].setPart(0,1,1,0);
		brick[6].setPart(0,2,0,1);
		brick[6].setPart(0,3,-1,1);
		//     [0]
		//     [1][2]
		//        [3]
		brick[6].setPart(1,0,0,0);
		brick[6].setPart(1,1,0,1);
		brick[6].setPart(1,2,1,1);
		brick[6].setPart(1,3,1,2);
	}

	void renderBrick(boolean b)
	{
		Dimension d;
		for(int i=0;i<brick[curbrick].numcells;i++)
		{
			d=brick[curbrick].getPart(curorient,i);
			if(b)
				cell[curloc.width+d.width][curloc.height+d.height].setCell(CELL_OCCUPIED,brick[curbrick].color);
			else
				cell[curloc.width+d.width][curloc.height+d.height].setCell(CELL_EMPTY,Color.black);
		}
	}

	boolean collisionDetect(Dimension loc, int orient)
	{
		Dimension d;
		int x,y;
		for(int i=0;i<brick[curbrick].numcells;i++)
		{
			d=brick[curbrick].getPart(orient,i);
			y=loc.height+d.height;
			x=loc.width+d.width;
			if(x<0 || x>NUMCOLS-1 || y<0 || y>NUMROWS-1)
				return true;
			if(cell[x][y].state!=CELL_EMPTY)
				return true;
		}
		return false;
	}

	public void moveLeft()
	{
		if(thread==null)
			return;
		Dimension loc=new Dimension();
		loc.setSize(curloc.width-1,curloc.height);
		moveBrick(loc,curorient);
		repaint();
	}

	public void moveRight()
	{
		if(thread==null)
			return;
		Dimension loc=new Dimension();
		loc.setSize(curloc.width+1,curloc.height);
		moveBrick(loc,curorient);
		repaint();
	}

	public void moveDown()
	{
		if(thread==null)
			return;
		Dimension loc=new Dimension();
		loc.setSize(curloc.width,curloc.height+1);
		if(moveBrick(loc,curorient))
		{
			// can't move down anymore, do the following things...
			collapse();
			// game over?
			if(curloc.equals(brick[curbrick].startloc))
			{
				JTetris.message("GAME OVER!");
				stop();
			}
			// start with a new brick
			curbrick=fetchBrick();
			curorient=0;
			curloc.setSize(brick[curbrick].startloc);
			// game over?
			if(collisionDetect(curloc,curorient))
			{
				JTetris.message("GAME OVER!");
				stop();
			}
		}
		repaint();
	}

	public void rotate()
	{
		if(thread==null)
			return;
		int orient=(curorient+1)%brick[curbrick].numorients;
		moveBrick(curloc,orient);
		repaint();
	}

	void collapse()
	{
		boolean bcomplete;
		int numcomplete=0;
		for(int i=NUMROWS-1;i>=0;i--)
		{
			bcomplete=true;
			for(int j=0;j<NUMCOLS;j++)
			{
				if(cell[j][i].state==CELL_EMPTY)
				{
					bcomplete=false;
					break;
				}
			}
			if(bcomplete)
			{
				numcomplete++;
				shiftDown(i);
				i++;
			}
		}

		if(numcomplete>0)
		{
			JTetris.setScore(score+=numcomplete);
			if(numcomplete==1)
			{
				JTetris.message("ISA LANG?");
			}
			else if(numcomplete==2)
			{
				JTetris.message("OKS!");
			}
			else if(numcomplete==3)
			{
				JTetris.message("GALING!");
			}
			else //if(numcomplete==3)
			{
				JTetris.message("BILIB!");
			}
		}
	}

	void shiftDown(int rowno)
	{
		for(int i=rowno;i>0;i--)
		{
			for(int j=0;j<NUMCOLS;j++)
			{
				cell[j][i].state=cell[j][i-1].state;
				cell[j][i].color=cell[j][i-1].color;
			}
		}
	}

	int fetchBrick()
	{
		return (int)(Math.random()*brick.length);
	}

	public synchronized boolean moveBrick(Dimension loc, int orient)
	{
		renderBrick(false);
		if(collisionDetect(loc,orient))
		{
			renderBrick(true);
			//repaint();
			return true;
		}
		curloc.setSize(loc);
		curorient=orient;
		renderBrick(true);
		//repaint();
		return false;
	}

	public boolean isRunning()
	{
		return (thread==null) ? false : true;
	}

	public void start(int d)
	{
		if(thread==null)
		{
			delay=d;
			thread=new Thread(this);
			thread.start();
		}
	}

	public void stop()
	{
		if(thread!=null)
		{
			thread.stop();
			thread=null;
		}
	}

	public void suspend()
	{
		if(thread!=null)
		{
			thread.suspend();
		}
	}

	public void resume()
	{
		if(thread!=null)
		{
			thread.resume();
		}
	}

	public void run()
	{
		// prepare offscreen image
		offsize=getSize();
		offimg=createImage(offsize.width,offsize.height);
		offg=offimg.getGraphics();
		cellsize.setSize(offsize.width/NUMCOLS,offsize.height/NUMROWS);

		// initialize cells
		for(int i=0;i<NUMCOLS;i++)
		{
			for(int j=0;j<NUMROWS;j++)
			{
				cell[i][j]=new JCell(CELL_EMPTY,Color.black);
			}
		}

		// starting brick
		curbrick=fetchBrick();
		curorient=0;
		curloc.setSize(brick[curbrick].startloc);
		renderBrick(true);
		repaint();

		score=0;
		JTetris.setScore(score);
		JTetris.message("");

		while(true)
		{
			moveDown();
			try
			{
				thread.sleep(delay);
			}
			catch(InterruptedException e)
			{
			}
		}
	}

	public synchronized void update(Graphics g)
	{
		offg.setColor(Color.black);
		offg.fillRect(0,0,offsize.width,offsize.height);

		for(int i=0;i<NUMCOLS;i++)
		{
			for(int j=0;j<NUMROWS;j++)
			{
				if(cell[i][j].state==CELL_OCCUPIED)
				{
					offg.setColor(cell[i][j].color);
					offg.fillRect(i*cellsize.width,j*cellsize.height,cellsize.width,cellsize.height);
					offg.setColor(Color.white);
					offg.drawRect(i*cellsize.width,j*cellsize.height,cellsize.width,cellsize.height);
					offg.setColor(Color.black);
				}
			}
		}
		g.drawImage(offimg,0,0,this);
	}
}

/**
 */
public class JTetris extends Applet
{
	/**
	 * The entry point for the applet. 
	 */
	public void init()
	{
		initForm();

		usePageParams();

		// TODO: Add any constructor code after initForm call.
	}

	/**
	 * Reads parameters from the applet's HTML host and sets applet
	 * properties.
	 */
	private void usePageParams()
	{
	}

	static TextField tfMessage=new TextField(10);
	static TextField tfScore=new TextField(4);
	static Choice cSpeed=new Choice();
	Panel pnStatus=new Panel();
	JTetrisCore cvTetris=new JTetrisCore();

	/**
	 * Intializes values for the applet and its components
	 */
	void initForm()
	{
		this.setLayout(new GridLayout(1,2,0,0));

		pnStatus.setBackground(Color.blue);
		//pnStatus.setEnabled(false);
		tfMessage.setBackground(Color.white);
		pnStatus.add(tfMessage);
		tfScore.setBackground(Color.white);
		pnStatus.add(tfScore);
		cSpeed.add("500");
		cSpeed.add("300");
		cSpeed.add("100");
		cSpeed.add("50");
		pnStatus.add(cSpeed);
		cvTetris.setBackground(Color.black);
		cvTetris.setEnabled(false);

		this.add(pnStatus);
		this.add(cvTetris);

		this.addKeyListener(new KL());

		this.requestFocus();
	}

	public static void message(String s)
	{
		tfMessage.setText(s);
	}

	public static void setScore(int s)
	{
		tfScore.setText(String.valueOf(s));
	}

	class KL extends KeyAdapter
	{
		public void keyPressed(KeyEvent evt)
		{
			int keyCode=evt.getKeyCode();

			if(keyCode==KeyEvent.VK_S)
			{
				cvTetris.stop();
				cvTetris.start(Integer.parseInt(cSpeed.getSelectedItem()));
			}
			else if(keyCode==KeyEvent.VK_P)
			{
				cvTetris.suspend();
			}
			else if(keyCode==KeyEvent.VK_C)
			{
				cvTetris.resume();
			}
			else if(keyCode==KeyEvent.VK_UP)
			{
				cvTetris.rotate();
			}
			else if(keyCode==KeyEvent.VK_LEFT)
			{
				cvTetris.moveLeft();
			}
			else if(keyCode==KeyEvent.VK_RIGHT)
			{
				cvTetris.moveRight();
			}
			else if(keyCode==KeyEvent.VK_DOWN)
			{
				cvTetris.moveDown();
			}
		}
	}

	public void destroy()
	{
		cvTetris.stop();
	}

	public static void main(String[] args)
	{
		JTetris jt=new JTetris();
		jt.setSize(400,400);
		jt.init();
		JFrame jf=new JFrame("JTetris 1.0");
		jf.setLayout(new GridLayout(1,1));
		jf.setSize(410,410);
		jf.add(jt);
		jf.show();
	}
}

class JFrame extends Frame implements WindowListener
{
	public JFrame()
	{
		super();
		addWindowListener( this );
	};

	public JFrame(String title)
	{
		super(title);
		addWindowListener( this );
	};

	public void windowClosing( WindowEvent evt )
	{
		((JTetris)getComponent(0)).destroy();
		System.exit(0);
	};

	public void windowOpened( WindowEvent evt )
	{
	};

	public void windowClosed( WindowEvent evt )
	{
	};

	public void windowDeiconified( WindowEvent evt )
	{
	};

	public void windowActivated( WindowEvent evt )
	{
	};

	public void windowIconified( WindowEvent evt )
	{
	};

	public void windowDeactivated( WindowEvent evt )
	{
	};
}

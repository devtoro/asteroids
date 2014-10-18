import java.applet.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.util.*;

public class Asteroids extends Applet implements Runnable, KeyListener {
	//the main thread becomes the main loop
	Thread gameloop;

	//doublebuffer
	BufferedImage backbuffer;

	//main drawing object for buffer
	Graphics2D g2d;

	//toggle for drawing bounding boxes
	boolean showBounds = false;

	//create the asteroid array
	int ASTEROIDS = 20;
	Asteroid[] ast = new Asteroid[ASTEROIDS];

	//create the bullet array
	int BULLETS = 10;
	Bullet[] bullet = new Bullet[BULLETS];
	int currentBullet = 0;

	//the player's ship
	Ship ship = new Ship();

	//identity transform
	AffineTransform identity = new AffineTransform();

	//random number generator
	Random rand = new Random();

	public void init() {
		//create the back buffer for smooth graphics
		backbuffer = new BufferedImage(640,480, BufferedImage.TYPE_INT_RGB);
		g2d = backbuffer.createGraphics();
		
		//set up the ship
		ship.setX(320);
		ship.setY(240);

		//set up the bullets
		for (int n = 0; n < BULLETS; n++){
			bullet[n] = new Bullet();
		}

		//create the asteroids
		for(int n = 0; n < ASTEROIDS; n++) {
			ast[n] = new Asteroid();
			ast[n].setRotationVelocity(rand.nextInt(3) + 1);
			ast[n].setX((double)rand.nextInt(600) + 20);
			ast[n].setY((double)rand.nextInt(440) + 20);
			ast[n].setMoveAngle(rand.nextInt(360));
			double ang = ast[n].getMoveAngle() - 90;
			ast[n].setVelX(calcAngleMoveX(ang));
			ast[n].setVelY(calcAngleMoveY(ang));
		}

		//start the user input listener

		addKeyListener(this);
	}

		//applet update event to redraw the screen
		public void update(Graphics g) {
			//start off transforms at identity

			g2d.setTransform(identity);

			//erase the background
			g2d.setPaint(Color.BLACK);
			g2d.fillRect(0,0,getSize().width, getSize().height);

			//print some status information
			g2d.setColor(Color.WHITE);
			g2d.drawString("Ship: " + Math.round(ship.getX()) + "," + Math.round(ship.getY()) , 5 , 10);
			g2d.drawString("Move Angle: " + Math.round(ship.getMoveAngle()) + 90, 5, 25);
			g2d.drawString("Face angle: " + Math.round(ship.getFaceAngle()), 4, 40);

			//draw the game graphics
			drawShip();
			drawBullets();
			drawAsteroids();

			//repaint the applet window
			paint(g);
		}

		//drawShip called by applet update event
		public void drawShip() {
			g2d.setTransform(identity);
			g2d.translate(ship.getX(), ship.getY());
			g2d.rotate(Math.toRadians(ship.getFaceAngle()));
			g2d.setColor(Color.ORANGE);
			g2d.fill(ship.getShape());
		}

		//drawBullets called by applet update
		public void drawBullets() {
			//iterate through the array of bullets
			for (int n = 0; n < BULLETS; n++) {
				//is this bullet currently in use?
				if(bullet[n].isAlive()) {
					//draw the bullet
					g2d.setTransform(identity);
					g2d.translate(bullet[n].getX(),bullet[n].getY());
					g2d.setColor(Color.MAGENTA);
					g2d.draw(bullet[n].getShape());
				}
			}
		}

		//draw asteroids called by the applet update method

	public void drawAsteroids() {
		//itterate through the asteroids array
		for(int n = 0; n < ASTEROIDS; n++) {
			//is this asteroid being used?
			if(ast[n].isAlive()) {
				//draw the asteroid
				g2d.setTransform(identity);
				g2d.translate(ast[n].getX(), ast[n].getY());
				g2d.rotate(Math.toRadians(ast[n].getMoveAngle()));
				g2d.setColor(Color.DARK_GRAY);
				g2d.fill(ast[n].getShape());
			}
		}
	}

	//applet window repaint event called bu applet update 
	public void paint(Graphics g) {
		//draw the backBuffer onto the applet window.
		g.drawImage(backbuffer,0,0,this);
	}

	//thread start event.

public void start() {
	//create the gameloop thread for real-time updates
	gameloop = new Thread(this);
	gameloop.start();
}

//thread run event (game loop)
public void run() {
	//acquire the current thread.
	Thread t = Thread.currentThread();

	//keep going as long as the thread is alive
	while(t == gameloop) {
		try {
			//update game loop
			gameUpdate();

			//target fraerate is 50fps
			Thread.sleep(20);
		}
		catch(InterruptedException e) {
			e.printStackTrace();
		}
		repaint();
	}
}

//thread stop event
public void stop() {
	//kill the game loop thread
	gameloop = null;
}

//move and animate the objects in the game - called by the gameloop (run method)
public void gameUpdate() {
	updateShip();
	updateBullets();
	updateAsteroids();
	checkCollisions();
}

//UPDATING METHODS

//update ship called in gameUpdate based on Velocity
public void updateShip() {
	//update x position
	ship.incX(ship.getVelX());

	//wrap around left/right
	if(ship.getX() < -10)
		ship.setX(getSize().width + 10);
	else if (ship.getX() > getSize().width + 10)
		ship.setX(-10);

	//update Y position
	ship.incY(ship.getVelY());

	//wrap around top/bottom
	if(ship.getY() < -10 )
		ship.setY(getSize().height + 10);
	else if (ship.getY() > getSize().height + 10)
		ship.setY(-10);
}

//Updates the bulets base on velocity -- called bgameUpdate
public void updateBullets() {
	//move each of the bullets
	for(int n = 0; n < BULLETS; n++) {
		//is the bullet being used?
		if (bullet[n].isAlive()) {
			//update bullets X position
			bullet[n].incX(bullet[n].getVelX());

			//bullet disapears at left/right edge
			if (bullet[n].getX() < 0 || bullet[n].getX() > getSize().width) {
				bullet[n].setAlive(false);
			}

			//update bullet's y position
			bullet[n].incY(bullet[n].getVelY());

			//bullet disapears at top/bottom edge
			if (bullet[n].getY() < 0 || bullet[n].getY() > getSize().height) {
				bullet[n].setAlive(false);
			}
		}
	}
}

//update thhe asteroids based on the velocity
public void updateAsteroids() {
	//move and rotate asteroids
	for(int n = 0; n < ASTEROIDS; n ++) {

		//is the current asteroid being used?
		if(ast[n].isAlive()) {
			//update the asteroid's x value
			ast[n].incX(ast[n].getVelX());

			//warp the asteroid at the screen edges
			if (ast[n].getX() < -20)
				ast[n].setX(getSize().width + 20);
			else if (ast[n].getX() > getSize().width + 20)
				ast[n].setX(-20);

			//update the asteroids Y value
			ast[n].incY(ast[n].getVelY());

			//warp asteroid at screen edges
			if(ast[n].getY() < -20)
				ast[n].setY(getSize().height + 20);
			else if(ast[n].getY() > getSize().width + 20)
				ast[n].setY(-20);

			//update the asteroids rotation
			ast[n].incMoveAngle(ast[n].getRotationVelocity());

			//Keep the angle within the 0-359 degrees
			if (ast[n].getMoveAngle() < 0)
				ast[n].setMoveAngle(360 - ast[n].getRotationVelocity());
			else if(ast[n].getMoveAngle() > 359)
				ast[n].setMoveAngle(ast[n].getRotationVelocity());
		}
	}
}

//Test asteroids for collisions with bullet or ship
public void checkCollisions() {
	//iterrate through the asteroids array
	for( int m = 0; m < ASTEROIDS; m++) {
		//is this asteroid being used?

		if (ast[m].isAlive()) {
			//check for collisions with bullet
			for(int n = 0; n < BULLETS; n++) {
				//is this bullet being used?
				if (bullet[n].isAlive()) {
					//perform the collision test

					if (ast[m].getBounds().contains(bullet[n].getX(), bullet[n].getY())) {
						bullet[n].setAlive(false);
						ast[m].setAlive(false);
						continue;
					}
				}
			}

			//check for collisions with ship
			if (ast[m].getBounds().intersects(ship.getBounds())) {
				ast[m].setAlive(false);
				ship.setX(320);
				ship.setY(240);
				ship.setFaceAngle(0);
				ship.setVelX(0);
				ship.setVelY(0);
				continue;
			}
		}
	}
}

//key listener events
public void keyReleased(KeyEvent k) { }
public void keyTyped(KeyEvent k) { }
public void keyPressed(KeyEvent k) {
	int keyCode = k.getKeyCode();
	switch (keyCode) {
		case KeyEvent.VK_LEFT:
			//left arrow rotates the ship 5 degrees
			ship.incFaceAngle(-5);
			if (ship.getFaceAngle() < 0) ship.setFaceAngle(360 - 5);
			break;
		case KeyEvent.VK_RIGHT:
			//right arrow rotates the ship 5 degrees
			ship.incFaceAngle(5);
			if (ship.getFaceAngle() > 360) ship.setFaceAngle(5);
			break;
		case KeyEvent.VK_UP:
			//up arrow adds thrust to the ship (1/10 normal speed)
			ship.setMoveAngle(ship.getFaceAngle() - 90);
			ship.incVelX(calcAngleMoveX(ship.getMoveAngle()) * 0.1);
			ship.incVelY(calcAngleMoveY(ship.getMoveAngle()) * 0.1);
			break;
		//Ctrl, enter or space can be used to fire weapon
		case KeyEvent.VK_CONTROL:
		case KeyEvent.VK_ENTER:
		case KeyEvent.VK_SPACE:
			//fire a bullet
			currentBullet++;
			if (currentBullet > BULLETS - 1) currentBullet = 0;
			bullet[currentBullet].setAlive(true);

			//point bullet same direction as ship
			bullet[currentBullet].setX(ship.getX());
			bullet[currentBullet].setY(ship.getY());
			bullet[currentBullet].setMoveAngle(ship.getFaceAngle() - 90);

			//fire bullet at angle fo the ship
			double angle = bullet[currentBullet].getMoveAngle();
			double svx = ship.getVelX();
			double svy = ship.getVelY();
			bullet[currentBullet].setVelX(svx + calcAngleMoveX(angle) * 2);
			bullet[currentBullet].setVelY(svy + calcAngleMoveY(angle)  *2);
			break;

	}
}

//calculate movement value base don direction angle
public double calcAngleMoveX(double angle) {
	return (double)(Math.cos(angle * Math.PI / 180));
}

//calculate Y movement value based on irection ange
public double calcAngleMoveY(double angle) {
	return (double)(Math.sin(angle*Math.PI / 180));
}

}
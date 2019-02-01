package export.add;

public class Sphere {

	// 중심 좌표.
	double cor_x;
	double cor_y;
	double cor_z;
	// 반지름
	double rad;

	public Sphere(double x,double y, double z, double r) {

		cor_x = x;
		cor_y = y;
		cor_z = z;

		// 반지름
		rad = r;

	}

	public double getCordinateX() {
		return cor_x;
	}

	public double getCordinateY() {
		return cor_y;
	}

	public double getCordinateZ() {
		return cor_z;
	}

	public double getRadius() {
		return rad;
	}

}

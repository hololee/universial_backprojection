package export.add;

public class CalculateData {

	// mm/us
	static double SOUND_SPEED = 0;

	// 작은 구
	Sphere[] series = { new Sphere(-18, 0, 15, 1.5), new Sphere(-9, 0, 15, 1.5), new Sphere(0, 0, 15, 1.5), new Sphere(9, 0, 15, 1.5), new Sphere(18, 0, 15, 1), new Sphere(0, -12, 15, 4),
			new Sphere(0, 12, 15, 4) };

	public CalculateData(double sound_speed) {

		SOUND_SPEED = sound_speed;

	}

	//calculate pd at point (x,y, time)
	public double calculatePd(int x, int y, double time) {

		double calculatedValue = 0;

		for (int m = -2; m <= 2; m++) {
			for (int n = -2; n <= 2; n++) {

				double subCal = 0;

				for (int i = 0; i < 7; i++) {
						subCal = subCal + function_U_Sigma(series[i].getRadius(), SensorToSphere(x, y, m, n, series[i]), time);
				}

				calculatedValue = calculatedValue + (subCal / 25.0d);

			}

		}

		return calculatedValue;
	}

	// 각 구 합산부분
	public double function_U_Sigma(double value, double distance, double time) {

		// x
		double calculation = value - Math.abs(distance - (SOUND_SPEED * time));

		// U(x)
		int subSum = 0;

		if (calculation >= 0) {
			subSum = 1;
		} else {
			subSum = 0;
		}

		return ((double) subSum * (distance - (SOUND_SPEED * time))) / (2 * distance);

	}

	// mm단위 Rij.
	public double SensorToSphere(int i, int j, int m, int n, Sphere sp) {

		// sensorCenter = di(m,n)
		// ------------------------원래 위치 mm 로 변환.
		double sensorCenterX = ((double) i) + (0.4d * (double) m);
		double sensorCenterY = ((double) j) + (0.4d * (double) n);

		double distance = Math.sqrt(Math.pow(sensorCenterX - sp.getCordinateX(), 2) + Math.pow(sensorCenterY - sp.getCordinateY(), 2) + Math.pow(0 - sp.getCordinateZ(), 2));

		//// Rij
		return distance;

	}

}

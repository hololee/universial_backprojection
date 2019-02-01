package export.add;

import java.util.logging.Logger;

import org.jtransforms.fft.DoubleFFT_1D;

import org.jzy3d.chart.AWTChart;
import org.jzy3d.chart.Chart;
import org.jzy3d.colors.Color;
import org.jzy3d.colors.ColorMapper;
import org.jzy3d.colors.colormaps.ColorMapRainbow;
import org.jzy3d.maths.Range;
import org.jzy3d.plot3d.builder.Builder;
import org.jzy3d.plot3d.builder.Mapper;
import org.jzy3d.plot3d.builder.concrete.OrthonormalGrid;
import org.jzy3d.plot3d.primitives.Shape;
import org.jzy3d.plot3d.rendering.canvas.Quality;

public class App {

	private final static Logger LOG = Logger.getGlobal();
	static CalculateData cal = new CalculateData(1.5); // s속도. mm/us

	static double P0_Size[][] = new double[61][61]; // 최종 결과,

	public static void main(String[] args) {
		LOG.info("Start");

		double[][][] pdXYT = new double[61][61][1200]; // pd 계산값.
		double[][] pdXYT_Straight = new double[61 * 61][1200]; // fft 를 위한 pd 직렬 값.
		Complex[][] fft_pdXYT_Straight = new Complex[61 * 61][1200]; // fft 한 복소수 값.
		Complex[][] fft_pdXYT_Straight2 = new Complex[61 * 61][1200]; // fft 한 복소수 값.
		double[] W = new double[1200]; // 주파수에 따른 필터 값.
		double[] K = new double[1200]; // 주파수에 따른 k 값.

		/**
		 * fft 함수의 결과가 복소수이며 배열 의 구조는 {real1, imag1, real2, imag2 ....} 순서이다. 따라서 배열의
		 * 길이는 2배로 직렬 구조로 표시하며 계산시에는 배열의 길이를 1/2 하여 합쳐서 나타낸다. => {Complex(real1, imag1),
		 * Complex(real2, imag2).....}
		 */
		
		double[][] ifft_pdXYT_Straight = new double[61 * 61][1200 * 2]; // 1차항 ifft 직렬 값.
		double[][] ifft_pdXYT_Straight2 = new double[61 * 61][1200 * 2]; // 2차항 ifft 직렬 값.
		Complex[][] ifft1_pdXYT = new Complex[61 * 61][1200]; // ifft 1차항 복소수 배열.
		Complex[][] ifft2_pdXYT = new Complex[61 * 61][1200]; // ifft 2차항 복소수 배열.
		Complex[][] bd = new Complex[61 * 61][1200]; // b_d 값.

		DoubleFFT_1D fftDo = new DoubleFFT_1D(1200);

		/// 위치에서 측정.
		int count = 0;
		for (int y = -30; y <= 30; y++) {
			for (int x = -30; x <= 30; x++) {

				// t는 0u초에서 부터 0.05u초 간격으로 60초 동안 측정. 0.05u 는 20MHz
				for (int t = 0; t < 1200; t++) {

					// 실제 시간.
					double time = t / (double) 20;
					pdXYT[x + 30][y + 30][t] = cal.calculatePd(x, y, time);

					/**
					 * fft 는 시간 고정일때의 좌표들을 fft 한 것을 반환. ex) t=0 일때 각 x,y 에 따른 좌표들을 퓨리에 변환. 각 시간 별로
					 * pd값 직렬화.
					 */
					pdXYT_Straight[count][t] = pdXYT[x + 30][y + 30][t];

				}

				count++;
			}
		}

		System.out.println("Size : " + count);

		LOG.info("FFT start");

		/**
		 * 
		 * 각 좌표에서 시간에 따라 변화하는 값들을 모아서 fft 처리한다. pd(d,t) 에서 pd(d,k) 로 변화 할때 t에 관한 함수를 fft
		 * 처리하는것이므로 시간에 따라 샘플링 된 값들을 변환.
		 * 
		 */

		// 각 좌표에서 시간에 따른 값들을 fft 계산 후 저장.
		for (int cordinate = 0; cordinate < 61 * 61; cordinate++) {

			// 각 좌표별 직렬화값.
			double[] ready_fft = new double[1200];
			// 1차 배열로 변환.
			for (int time = 0; time < 1200; time++) {
				ready_fft[time] = pdXYT_Straight[cordinate][time];
			}

			double[] fft = new double[1200 * 2];
			System.arraycopy(ready_fft, 0, fft, 0, ready_fft.length);

			// fft 계산.
			fftDo.realForwardFull(fft);

			// complex 형태로 변환후 저장.
			for (int a = 0; a < 1200; a++) {
				Complex cx = new Complex(fft[2 * a], fft[(2 * a) + 1]);
				fft_pdXYT_Straight[cordinate][a] = cx;

			}

		}

		/////////////////////////////////// pd 까지 이상 없음.

		LOG.info("w,k calculate start");
		/**
		 * 
		 * 60us 간격으로 샘플링
		 * 
		 * https://www.google.co.kr/imgres?imgurl=https://i.stack.imgur.com/yeW3v.jpg&imgrefurl=https://electronics.stackexchange.com/questions/12407/what-is-the-relation-between-fft-length-and-frequency-resolution&h=472&w=449&tbnid=T5TMVNMIJ0J2iM:&q=fft+frequency+resolution+and&tbnh=160&tbnw=152&usg=AI4_-kRrMOC78_qHFmy8GG_MxA7NCIApOQ&vet=12ahUKEwj_3ObXp-fdAhUX5bwKHaqzAjUQ9QEwAHoECAoQBg..i&docid=QZkG0nmn5x7Y8M&sa=X&ved=2ahUKEwj_3ObXp-fdAhUX5bwKHaqzAjUQ9QEwAHoECAoQBg
		 * 
		 * 시간축에서 전체 시간이 T 일때 fft 후 주파수 축에서의 bandWidth = 1/T 가 된다. 따라서 60us 간 측정한 함수를 fft
		 * 를 적용하면 bandWidth= 1/T 즉 1/60us = 50000/3(w 구할때의 주파수 간격)
		 * 
		 */

		// 필터 계산.
		double fc = 4000000;

		for (int w = 0; w < 1200; w++) {
			// 실제 주파수 간격.
			double realW = w * ((double) 50000 / (double) 3);

			K[w] = realW * (double) 2 * Math.PI / (double) 1500000;
			////////////////// k 이상 없음.
			if (Math.abs(realW) < fc) {
				W[w] = (double) 0.5 + ((double) 0.5 * Math.cos(Math.PI * (realW / fc)));
			} else {
				W[w] = (double) 0;
			}
			////////////////// w 이상 없음.

		}

		LOG.info("Pd filter calculate and array adjustment");
		// ffb1 ,ffb2 계산
		for (int i = 0; i < 61 * 61; i++) {
			for (int t = 0; t < 1200; t++) {
				fft_pdXYT_Straight[i][t] = fft_pdXYT_Straight[i][t].times(new Complex(W[t], 0));
				// 역변환 한 후 b1,b2 계산후 저장.
				fft_pdXYT_Straight2[i][t] = fft_pdXYT_Straight[i][t].times(new Complex(((double) K[t]), 0)).times(new Complex(0, -1));

				// 직렬 데이터 배열을 복소수 배열로 변환.
				// 실수 부분.
				ifft_pdXYT_Straight[i][2 * t] = fft_pdXYT_Straight[i][t].re();
				// 허수 부분.
				ifft_pdXYT_Straight[i][2 * t + 1] = fft_pdXYT_Straight[i][t].im();

				// 실수 부분.
				ifft_pdXYT_Straight2[i][2 * t] = fft_pdXYT_Straight2[i][t].re();
				// 허수 부분.
				ifft_pdXYT_Straight2[i][2 * t + 1] = fft_pdXYT_Straight2[i][t].im();

			}
		}

		LOG.info("Ifft start");
		/**
		 * 
		 * ifft 는 주파수 도메인 에서 타임 도메인으로 변겅이다. 따라서 주파수에 따라 변화하는 값들[fft 이전의 [t] 에서 같은 t 를 가진
		 * 값들의 모임]을 fft 로 돌려야 한다. F(w) => f(t)
		 * 
		 * 함수[coordinate][change of time or frequency]; fft 는 같은 좌표의 2차 배열순서의 모임으로 fft
		 * => {함수[fixed][0], ...... , 함수[fixed][1199]}
		 * 
		 */

		// 1차 배열로 변환.
		for (int straight = 0; straight < 61 * 61; straight++) {

			// 각 좌표별 직렬화값.
			double[] ifft1 = new double[1200 * 2];
			double[] ifft2 = new double[1200 * 2];
			// 1차 배열로 변환.
			for (int t = 0; t < (1200 * 2); t++) {
				ifft1[t] = ifft_pdXYT_Straight[straight][t];
				ifft2[t] = ifft_pdXYT_Straight2[straight][t]; // ifft_pdXYT_Straight 는 실수부 허수부가 반복되는 구조

			}

			// ifft
			fftDo.complexInverse(ifft1, true);
			fftDo.complexInverse(ifft2, true);

			// bd 계산.
			for (int t = 0; t < 1200; t++) {

				// real time.
				double time = t / (double) 20;

				ifft1_pdXYT[straight][t] = new Complex(ifft1[2 * t], ifft1[(2 * t) + 1]).times(new Complex(2, 0));
				ifft2_pdXYT[straight][t] = new Complex(ifft2[2 * t], ifft2[(2 * t) + 1]).times(new Complex(CalculateData.SOUND_SPEED * time, 0));
				// 홀수는 실수값,

				bd[straight][t] = ifft1_pdXYT[straight][t].minus(ifft2_pdXYT[straight][t]);

			}

		}

		// 최종 그리기.
		Complex P0[][] = new Complex[61][61];
		Complex p0_top[][] = new Complex[61][61];
		double p0_bot[][] = new double[61][61];

		LOG.info("Magnitude calculate start");

		for (int d_y = -30; d_y <= 30; d_y++) {
			for (int d_x = -30; d_x <= 30; d_x++) {

				Complex sum_top = new Complex(0, 0);
				double sum_bot = 0;

				int i = 0;

				for (int y = -30; y <= 30; y++) {
					for (int x = -30; x <= 30; x++) {

						double r_sub_d = Math.sqrt(Math.pow(x - d_x, 2) + Math.pow(y - d_y, 2) + Math.pow(15, 2));
						// double r_sub_d_y = (y - r_y) / r_sub_d;
						double r_sub_d_z = 15 / r_sub_d;
						// r-di 의 z 방향의 단위벡터 성분.

						/**
						 * 오메가 계산.
						 * 
						 * nSoi . (r-di)/|r-di| => r-di 단위벡터의 z축 방향 성분. nSoi 는 di 에서 측정면에 수직인 단위벡터. Si 는
						 * 디텍터 면적.
						 * 
						 * 따라서 (Si * 15) / |r-di|^3 과 같음.
						 * 
						 */

						// (0,0,1) * r_sub_d_z, noi : normal vector.
						// double noi = r_sub_d_z * 1;
						// double DoH = ((double) 4 / Math.pow(r_sub_d, 2)) * noi; // 오메가,
						// =2mm*2mm = 4;

						double DoH = 60 / (Math.pow(r_sub_d, 3));

						// 거리 / 속도 == 실제시간.
						// 실제시간 * 20 = interval.
						double interval = (r_sub_d / (double) 1.5) * 20;

						if (interval >= 0 && interval <= 1200) {
							sum_top = sum_top.plus(bd[i][(int) Math.round(interval)].times(new Complex(DoH, 0)));

							// System.out.println((Math.round(interval)) / 20 +"us");
							/**
							 * r_sub_d / 0.075 = count[0-1199] r_sub_d/ 1.5 = 실제 시간. == >r0 상의 측정점에서 측정
							 * 위치까지의 음파 전달 시간이 기존에 구해둔 시간에 해당되는 bd[좌표][t] 일때의 값으로 적용.
							 */

							sum_bot = sum_bot + DoH;
						}

						p0_top[30 + d_x][30 + d_y] = sum_top;
						p0_bot[30 + d_x][30 + d_y] = sum_bot;
						P0[30 + d_x][30 + d_y] = sum_top.divides(new Complex(sum_bot, 0)); // sum_top/sum_bot

						i++;
					}

				}

			}
		}

		// Calculate magnitude of P0 between -30 and 30;
		for (int x = -30; x <= 30; x++) {
			for (int y = -30; y <= 30; y++) {
				P0_Size[30 + x][30 + y] = P0[30 + x][30 + y].abs();
			}
		}

		LOG.info("Draw plot");

		// Define a function to plot
		Mapper mapper = new Mapper() {
			public double f(double x, double y) {
				return P0_Size[(int) x][(int) y];
			}
		};

		// Set range of plot
		Range range = new Range(0, 60);
		// unit => 1
		int steps = 60;

		// Create a surface drawing that function
		Shape surface = Builder.buildOrthonormal(new OrthonormalGrid(range, steps), mapper);
		surface.setColorMapper(new ColorMapper(new ColorMapRainbow(), surface.getBounds().getZmin(), surface.getBounds().getZmax(), new Color(1, 1, 1, .5f)));

		surface.setFaceDisplayed(true);
		surface.setWireframeDisplayed(true);
		surface.setWireframeWidth(0.1f);
		surface.setWireframeColor(Color.BLACK);

		// Create a chart and add the surface
		Chart chart = new AWTChart(Quality.Nicest);
		chart.add(surface);
		chart.addMouseCameraController();
		chart.open("plot", 800, 800);

		LOG.info("finish");

	}

}

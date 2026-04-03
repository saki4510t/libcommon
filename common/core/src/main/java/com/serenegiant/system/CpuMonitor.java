/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.serenegiant.system;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import androidx.annotation.NonNull;

/**
 * API26以降はCPU周波数の最高周波数に対する割合の平均値をCPU負荷として返すため高めに表示される
 * サーマルスロットリングでCPU周波数が制限されると見かけ上CPU負荷が低いように見えてしまうかも
 * Modified 2016 t_saki@serenegiant.com
 * Modified 2025 t_saki@serenegiant.com
 *
 * Simple CPU monitor.  The caller creates a CpuMonitor object which can then
 * be used via sampleCpuUtilization() to collect the percentual use of the
 * cumulative CPU capacity for all CPUs running at their nominal frequency.  3
 * values are generated: (1) getCpuCurrent() returns the use since the last
 * sampleCpuUtilization(), (2) getCpuAvg3() returns the use since 3 prior
 * calls, and (3) getCpuAvgAll() returns the use over all SAMPLE_SAVE_NUMBER
 * calls.
 *
 * <p>CPUs in Android are often "offline", and while this of course means 0 Hz
 * as current frequency, in this state we cannot even get their nominal
 * frequency.  We therefore tread carefully, and allow any CPU to be missing.
 * Missing CPUs are assumed to have the same nominal frequency as any close
 * lower-numbered CPU, but as soon as it is online, we'll get their proper
 * frequency and remember it.  (Since CPU 0 in practice always seem to be
 * online, this unidirectional frequency inheritance should be no problem in
 * practice.)
 *
 * <p>Caveats:
 *   o No provision made for zany "turbo" mode, common in the x86 world.
 *   o No provision made for ARM big.LITTLE; if CPU n can switch behind our
 *     back, we might get incorrect estimates.
 *   o This is not thread-safe.  To call asynchronously, create different
 *     CpuMonitor objects.
 *
 * <p>If we can gather enough info to generate a sensible result,
 * sampleCpuUtilization returns true.  It is designed to never through an
 * exception.
 *
 * <p>sampleCpuUtilization should not be called too often in its present form,
 * since then deltas would be small and the percent values would fluctuate and
 * be unreadable. If it is desirable to call it more often than say once per
 * second, one would need to increase SAMPLE_SAVE_NUMBER and probably use
 * Queue<Integer> to avoid copying overhead.
 *
 * <p>Known problems:
 *   1. Nexus 7 devices running Kitkat have a kernel which often output an
 *      incorrect 'idle' field in /proc/stat.  The value is close to twice the
 *      correct value, and then returns to back to correct reading.  Both when
 *      jumping up and back down we might create faulty CPU load readings.
 */

public final class CpuMonitor {
	private static final boolean DEBUG = false;	// set false on production
	private static final String TAG = "CpuMonitor";
	private static final int SAMPLE_SAVE_NUMBER = 10;  // Assumed to be >= 3.

//--------------------------------------------------------------------------------
	private static final class ProcStat {
		private long runTime;
		private long idleTime;

		private ProcStat(final long aRunTime, final long aIdleTime) {
			runTime = aRunTime;
			idleTime = aIdleTime;
		}

		private void set(final long aRunTime, final long aIdleTime) {
			runTime = aRunTime;
			idleTime = aIdleTime;
		}

		private void set(final ProcStat other) {
			runTime = other.runTime;
			idleTime = other.idleTime;
		}

		@NonNull
		@Override
		public String toString() {
			return "ProcStat{" +
				"runTime=" + runTime +
				",idleTime=" + idleTime +
				'}';
		}
	}

//--------------------------------------------------------------------------------
	private final int[] percentVec = new int[SAMPLE_SAVE_NUMBER];
	private int sum3 = 0;
	private int sum10 = 0;
	private long[] cpuMinFreq;
	private long[] cpuMaxFreq;
	private int cpusPresent;
	private double lastPercentFreq = -1;
	private int cpuCurrent;
	private int cpuAvg3;
	private int cpuAvgAll;
	private boolean initialized = false;
	private String[] minPath;
	private String[] maxPath;
	private String[] curPath;
	@NonNull
	private final ProcStat lastProcStat = new ProcStat(0L, 0L);
	@NonNull
	private final Map<String, Integer> mCpuTemps = new HashMap<>();
	private int mTempNum = 0;
	private float tempAve = 0;

	private void init() {
		try {
			final FileReader fin = new FileReader("/sys/devices/system/cpu/present");
			try {
				final BufferedReader rdr = new BufferedReader(fin);
				final Scanner scanner = new Scanner(rdr).useDelimiter("[-\n]");
				scanner.nextInt();  // Skip leading number 0.
				cpusPresent = 1 + scanner.nextInt();
				scanner.close();
			} catch (final Exception e) {
				Log.e(TAG, "Cannot do CPU stats due to /sys/devices/system/cpu/present parsing problem");
			} finally {
				fin.close();
			}
		} catch (FileNotFoundException e) {
			Log.e(TAG, "Cannot do CPU stats since /sys/devices/system/cpu/present is missing");
		} catch (IOException e) {
			Log.e(TAG, "Error closing file");
		}
		if (DEBUG) Log.v(TAG, "init:cpusPresent=" + cpusPresent);

		cpuMinFreq = new long [cpusPresent];
		cpuMaxFreq = new long [cpusPresent];
		minPath = new String[cpusPresent];
		maxPath = new String[cpusPresent];
		curPath = new String[cpusPresent];
		for (int i = 0; i < cpusPresent; i++) {
			cpuMinFreq[i] = 0;  // Frequency "not yet determined".
			cpuMaxFreq[i] = 0;  // Frequency "not yet determined".
			minPath[i] = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_min_freq"; // scaling_min_freq
			maxPath[i] = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/cpuinfo_max_freq"; // scaling_max_freq
			curPath[i] = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq";
		}
		if (DEBUG) Log.v(TAG, "init:minPath=" + Arrays.toString(minPath));
		if (DEBUG) Log.v(TAG, "init:maxPath=" + Arrays.toString(maxPath));
		if (DEBUG) Log.v(TAG, "init:curPath=" + Arrays.toString(curPath));

		lastProcStat.set(0, 0);

		mCpuTemps.clear();
		mTempNum = 0;
		for (int i = 0; i < 50; i++) {
			final String path = "/sys/class/hwmon/hwmon" + i;
			final File dir = new File(path);
			if (dir.exists() && dir.canRead()) {
				mCpuTemps.put(path, 0);
				mTempNum++;
			}
		}
		if (DEBUG) Log.v(TAG, "init:cpuTemp=" + mCpuTemps);

		initialized = true;
	}

	/**
	 * Re-measure CPU use.  Call this method at an interval of around 1/s.
	 * This method returns true on success.  The fields
	 * cpuCurrent, cpuAvg3, and cpuAvgAll are updated on success, and represents:
	 * cpuCurrent: The CPU use since the last sampleCpuUtilization call.
	 * cpuAvg3: The average CPU over the last 3 calls.
	 * cpuAvgAll: The average CPU over the last SAMPLE_SAVE_NUMBER calls.
	 * CPU周辺の温度を取得するのが少し重いのでUIスレッドからは呼び出さないほうが良い
	 */
	public boolean sampleCpuUtilization() {
		long lastSeenMaxFreq = 0;
		long cpufreqCurSum = 0;
		long cpufreqMaxSum = 0;

		if (!initialized) {
			init();
		}

		if (!initialized) {
			return false;
		}

		if (BuildCheck.isAPI26()) {
			return sampleCpuUtilizationNew();
		} else {
			return sampleCpuUtilizationOld();
		}
	}

	public int getCpuCurrent() {
		return cpuCurrent;
	}

	public int getCpuAvg3() {
		return cpuAvg3;
	}

	public int getCpuAvgAll() {
		return cpuAvgAll;
	}

	public int getTempNum() {
		return mTempNum;
	}

	public int getTemp(final int ix) {
		int result = 0;
		if ((ix >= 0) && (ix < mTempNum)) {
			final String path = "/sys/class/hwmon/hwmon" + ix;
			if (mCpuTemps.containsKey(path)) {
				result = mCpuTemps.get(path);
			}
		}
		return result;
	}

	public float getTempAve() {
		return tempAve;
	}

	/**
	 * Re-measure CPU use.  Call this method at an interval of around 1/s.
	 * This method returns true on success.  The fields
	 * cpuCurrent, cpuAvg3, and cpuAvgAll are updated on success, and represents:
	 * cpuCurrent: The CPU use since the last sampleCpuUtilization call.
	 * cpuAvg3: The average CPU over the last 3 calls.
	 * cpuAvgAll: The average CPU over the last SAMPLE_SAVE_NUMBER calls.
	 * CPU周辺の温度を取得するのが少し重いのでUIスレッドからは呼び出さないほうが良い
	 */
	private boolean sampleCpuUtilizationOld() {
		double percentFreq = updateCpuFrequency();
		if (percentFreq == 0) {
			return false;
		}

		final ProcStat procStat = readIdleAndRunTime();
		if (procStat == null) {
			return sampleCpuUtilizationNew();
		}

		final long diffRunTime = procStat.runTime - lastProcStat.runTime;
		final long diffIdleTime = procStat.idleTime - lastProcStat.idleTime;

		// Save new measurements for next round's deltas.
		lastProcStat.set(procStat);

		final long allTime = diffRunTime + diffIdleTime;
		int percent = allTime == 0 ? 0 : (int) Math.round(percentFreq * diffRunTime / allTime);
		percent = Math.max(0, Math.min(percent, 100));

		// Subtract old relevant measurement, add newest.
		sum3 += percent - percentVec[2];
		// Subtract oldest measurement, add newest.
		sum10 += percent - percentVec[SAMPLE_SAVE_NUMBER - 1];

		// Rotate saved percent values, save new measurement in vacated spot.
		for (int i = SAMPLE_SAVE_NUMBER - 1; i > 0; i--) {
			percentVec[i] = percentVec[i - 1];
		}
		percentVec[0] = percent;

		cpuCurrent = percent;
		cpuAvg3 = sum3 / 3;
		cpuAvgAll = sum10 / SAMPLE_SAVE_NUMBER;

		tempAve = 0;
		float tempCnt = 0;
		for (final String path: mCpuTemps.keySet()) {
			final File dir = new File(path);
			if (dir.exists() && dir.canRead()) {
				final File file = new File(dir, "temp1_input");
				if (file.exists() && file.canRead()) {
					final int temp = (int)readFreqFromFile(file.getAbsolutePath());
					mCpuTemps.put(path, temp);
					if (temp > 0) {
						tempCnt++;
						tempAve += temp > 1000 ? temp / 1000.0f : temp;
					}
				}
			}
		}
		if (tempCnt > 0) {
			tempAve /= tempCnt;
		}
		return true;
	}

	/**
	 * Android8/API26以降では通常のアプリから/proc/statが読み込めなくなっている
	 * (vmstat等もだめ)なので代わりにCPU周波数の割合をCPU負荷率として返す
	 */
	private boolean sampleCpuUtilizationNew() {
		double percentFreq = updateCpuFrequency();
		if (percentFreq == 0) {
			return false;
		}
		int percent = Math.round((float)percentFreq);
		percent = Math.max(0, Math.min(percent, 100));

		// Subtract old relevant measurement, add newest.
		sum3 += percent - percentVec[2];
		// Subtract oldest measurement, add newest.
		sum10 += percent - percentVec[SAMPLE_SAVE_NUMBER - 1];

		// Rotate saved percent values, save new measurement in vacated spot.
		for (int i = SAMPLE_SAVE_NUMBER - 1; i > 0; i--) {
			percentVec[i] = percentVec[i - 1];
		}
		percentVec[0] = percent;

		cpuCurrent = percent;
		cpuAvg3 = sum3 / 3;
		cpuAvgAll = sum10 / SAMPLE_SAVE_NUMBER;

		tempAve = 0;
		float tempCnt = 0;
		for (final String path: mCpuTemps.keySet()) {
			final File dir = new File(path);
			if (dir.exists() && dir.canRead()) {
				final File file = new File(dir, "temp1_input");
				if (file.exists() && file.canRead()) {
					final int temp = (int)readFreqFromFile(file.getAbsolutePath());
					mCpuTemps.put(path, temp);
					if (temp > 0) {
						tempCnt++;
						tempAve += temp > 1000 ? temp / 1000.0f : temp;
					}
				}
			}
		}
		if (tempCnt > 0) {
			tempAve /= tempCnt;
		}
		return true;
	}

	/**
	 * CPU周波数の取得計算処理を分離
	 * 元々は周波数比率=現在のCPU周波数の合計÷最大周波数の合計
	 * だったのを
	 * 周波数比率=(現在のCPU周波数-最小周波数)の合計÷(最大周波数-最小周波数)の合計
	 * に変更している
	 * @return
	 */
	private double updateCpuFrequency() {
		long lastSeenMinFreq = 0;
		long lastSeenMaxFreq = 0;
		long cpufreqCurSum = 0;
		long cpufreqMaxSum = 0;

		for (int i = 0; i < cpusPresent; i++) {
			/*
			 * For each CPU, attempt to first read its max frequency, then its
			 * current frequency.  Once as the max frequency for a CPU is found,
			 * save it in cpuFreq[].
			 */

			if (cpuMinFreq[i] == 0) {
				// We have never found this CPU's max frequency.  Attempt to read it.
				long cpufreqMin = readFreqFromFile(minPath[i]);
				if (cpufreqMin > 0) {
					lastSeenMinFreq = cpufreqMin;
					cpuMinFreq[i] = cpufreqMin;
					minPath[i] = null;  // Kill path to free its memory.
				}
			} else {
				lastSeenMinFreq = cpuMinFreq[i];  // A valid, previously read value.
			}

			if (cpuMaxFreq[i] == 0) {
				// We have never found this CPU's max frequency.  Attempt to read it.
				long cpufreqMax = readFreqFromFile(maxPath[i]);
				if (cpufreqMax > 0) {
					lastSeenMaxFreq = cpufreqMax;
					cpuMaxFreq[i] = cpufreqMax;
					maxPath[i] = null;  // Kill path to free its memory.
				}
			} else {
				lastSeenMaxFreq = cpuMaxFreq[i];  // A valid, previously read value.
			}

			long cpufreqCur = readFreqFromFile(curPath[i]);
			cpufreqCurSum += (cpufreqCur - lastSeenMinFreq);

			/* Here, lastSeenMaxFreq might come from
			 * 1. cpuFreq[i], or
			 * 2. a previous iteration, or
			 * 3. a newly read value, or
			 * 4. hypothetically from the pre-loop dummy.
			 */
			cpufreqMaxSum += (lastSeenMaxFreq - lastSeenMinFreq);
		}

		if (cpufreqMaxSum == 0) {
			Log.e(TAG, "Could not read max frequency for any CPU");
			return 0;
		}

		/*
		 * Since the cycle counts are for the period between the last invocation
		 * and this present one, we average the percentual CPU frequencies between
		 * now and the beginning of the measurement period.  This is significantly
		 * incorrect only if the frequencies have peeked or dropped in between the
		 * invocations.
		 */
		final double newPercentFreq = 100.0 * cpufreqCurSum / cpufreqMaxSum;
		final double percentFreq = lastPercentFreq > 0 ? (lastPercentFreq + newPercentFreq) * 0.5 : newPercentFreq;
		lastPercentFreq = newPercentFreq;
		return percentFreq;
	}

	/**
	 * Read a single integer value from the named file.  Return the read value
	 * or if an error occurs return 0.
	 * big.LITTLEで落ちているCPUの周波数は取得できない
	 */
	private long readFreqFromFile(String fileName) {
		long number = 0;
		try {
			final FileReader fin = new FileReader(fileName);
			try {
				final BufferedReader rdr = new BufferedReader(fin);
				final Scanner scannerC = new Scanner(rdr);
				number = scannerC.nextLong();
				scannerC.close();
			} catch (final Exception e) {
				// CPU presumably got offline just after we opened file.
			} finally {
				fin.close();
			}
		} catch (final FileNotFoundException e) {
			// CPU is offline, not an error.
		} catch (final Exception e) {
			if (DEBUG) Log.e(TAG, "Error closing file");
		}
		return number;
	}

	/*
	 * Read the current utilization of all CPUs using the cumulative first line
	 * of /proc/stat.
	 */
	private ProcStat readIdleAndRunTime() {
		long runTime;
		long idleTime;
		try {
			// API26以降ではruntime.execでcat /proc/statへアクセスするのもvmstatへアクセスするのもだめ
			final FileReader fin = new FileReader("/proc/stat");
			try {
				final BufferedReader rdr = new BufferedReader(fin);
				final Scanner scanner = new Scanner(rdr);
				scanner.next();
				long user = scanner.nextLong();
				long nice = scanner.nextLong();
				long sys = scanner.nextLong();
				runTime = user + nice + sys;
				idleTime = scanner.nextLong();
				scanner.close();
			} catch (final Exception e) {
				Log.e(TAG, "Problems parsing /proc/stat");
				return null;
			} finally {
				fin.close();
			}
		} catch (final FileNotFoundException e) {
			Log.e(TAG, "Cannot open /proc/stat for reading");
			return null;
		} catch (final IOException e) {
			Log.e(TAG, "Problems reading /proc/stat");
			return null;
		}
		return new ProcStat(runTime, idleTime);
	}
}

/**********************************************************************************************************************
 * Copyright (c) 2010, Institute of Telematics, University of Luebeck                                                 *
 * All rights reserved.                                                                                               *
 *                                                                                                                    *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the   *
 * following conditions are met:                                                                                      *
 *                                                                                                                    *
 * - Redistributions of source code must retain the above copyright notice, this list of conditions and the following *
 *   disclaimer.                                                                                                      *
 * - Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the        *
 *   following disclaimer in the documentation and/or other materials provided with the distribution.                 *
 * - Neither the name of the University of Luebeck nor the names of its contributors may be used to endorse or        *
 *   promote products derived from this software without specific prior written permission.                           *
 *                                                                                                                    *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, *
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE      *
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,         *
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE *
 * GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF    *
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY   *
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.                                *
 **********************************************************************************************************************/

package de.uniluebeck.itm.wsn.deviceutils.observer;

import com.google.common.io.ByteStreams;
import com.google.common.io.Closeables;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import org.apache.commons.lang.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class DeviceCsvProviderImpl implements DeviceCsvProvider {

	private static final Logger log = LoggerFactory.getLogger(DeviceCsvProvider.class);

	@Override
	public String getDeviceCsv() {
		if (SystemUtils.IS_OS_LINUX) {
			return getCsv("devicelist-linux");
		} else if (SystemUtils.IS_OS_MAC_OSX) {
			return getCsv("devicelist-macosx");
		} else if (SystemUtils.IS_OS_WINDOWS_XP) {
			return getCsv("devicelist-windowsxp.exe");
		}
		throw new RuntimeException(
				"OS " + SystemUtils.OS_NAME + " " + SystemUtils.OS_VERSION +
						"(" + SystemUtils.OS_ARCH + ") is currently not supported!"
		);
	}

	private String getCsv(final String scriptName) {

		File tmpFile = copyScriptToTmpFile(scriptName);

		try {

			ProcessBuilder pb = new ProcessBuilder(tmpFile.getAbsolutePath(), "-c");
			Process p = pb.start();
			final String csv = new String(ByteStreams.toByteArray(p.getInputStream()));
			if (!tmpFile.delete()) {
				tmpFile.deleteOnExit();
			}
			return csv;

		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private File copyScriptToTmpFile(final String scriptName) {

        InputStream source = null;
		try {

            File to = File.createTempFile("devicelist", "");
            source = getClass().getClassLoader().getResourceAsStream(scriptName);
            Files.asByteSink(to, FileWriteMode.APPEND).writeFrom(source);

			if (!to.setExecutable(true)) {
				throw new RuntimeException("Could not set executable flag on devicelist script!");
			}
			return to;

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
            Closeables.closeQuietly(source);
        }

	}

}

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

import de.uniluebeck.itm.wsn.drivers.core.MacAddress;

import static com.google.common.base.Preconditions.checkNotNull;

public class DeviceInfo {

	String type;

	String port;

	String reference;

	MacAddress macAddress;

	public DeviceInfo(final String type, final String port, final String reference, final MacAddress macAddress) {

		checkNotNull(type);
		checkNotNull(port);

		this.type = type;
		this.port = port;

		this.reference = reference;
		this.macAddress = macAddress;
	}

	public MacAddress getMacAddress() {
		return macAddress;
	}

	public String getPort() {
		return port;
	}

	public String getReference() {
		return reference;
	}

	public String getType() {
		return type;
	}

	@Override
	public boolean equals(final Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		final DeviceInfo that = (DeviceInfo) o;

		if (macAddress != null ? !macAddress.equals(that.macAddress) : that.macAddress != null) {
			return false;
		}
		if (!port.equals(that.port)) {
			return false;
		}
		if (reference != null ? !reference.equals(that.reference) : that.reference != null) {
			return false;
		}
		if (!type.equals(that.type)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + port.hashCode();
		result = 31 * result + (reference != null ? reference.hashCode() : 0);
		result = 31 * result + (macAddress != null ? macAddress.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "DeviceInfo{" +
				"type='" + type + '\'' +
				", port='" + port + '\'' +
				", reference='" + reference + '\'' +
				", macAddress=" + macAddress +
				'}';
	}
}
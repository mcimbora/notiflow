/*
 *   Copyright (C) 2021 the original author or authors.
 *
 *   This file is part of Notiflow
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.obj.nc.functions.processors.messageAggregator.correlations;

import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.messaging.Message;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EventIdBasedCorrelationStrategy implements CorrelationStrategy {

	@Override
	public Object getCorrelationKey(Message<?> m) {
		com.obj.nc.domain.message.Message ncMessage = (com.obj.nc.domain.message.Message)m.getPayload();

		log.info("Correlating message {} based on their eventIds {}",m , ncMessage.getPreviousEventIds());
		return ncMessage.getPreviousEventIds();
	}

}

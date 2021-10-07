/*
 *   Copyright (C) 2021 the original author or authors.
 *
 *   This file is part of Notiflow
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Lesser General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public License
 *   along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.obj.nc.functions.processors;

import java.util.Optional;
import java.util.function.Function;

import com.obj.nc.exceptions.PayloadValidationException;
import com.obj.nc.functions.PreCondition;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class ProcessorFunction<IN, OUT> implements Function<IN, OUT> {

	@Override
	public OUT apply(IN input) {
		OUT	returnValue = doPreConditionCheckAndExecute(input);

		return returnValue;
	}

	private OUT doPreConditionCheckAndExecute(IN input) {
        log.debug(this.getClass().getSimpleName() + ".preCondition START");
		Optional<PayloadValidationException> error = preCondition().apply(input);
        log.debug(this.getClass().getSimpleName() + ".preCondition END");
		
		if (error.isPresent()) {
			throw error.get();
		}

        log.debug(this.getClass().getSimpleName() + ".execution START: {}", input);
		OUT result = execution().apply(input);    
        log.debug(this.getClass().getSimpleName() + ".execution END: {}", result);

        return result;
	}
	
	public abstract PreCondition<IN> preCondition();
	
	public abstract Function<IN, OUT> execution();

}
 
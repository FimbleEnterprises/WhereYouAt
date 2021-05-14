package com.fimbleenterprises.whereuat.rest_api;

import com.google.gson.Gson;

import java.util.ArrayList;

import androidx.annotation.Nullable;

public class Requests {

    public static class Request {

        // region CONSTANTS
        public static final String UPDATE_TRIP = "updatetrip";
        public static final String CREATE_NEW_TRIP = "createnewtrip";
        public static final String UPSERT_USER = "upsertuser";
        public static final String UPSERT_FCM_TOKEN = "upsertfcmtoken";
        public static final String JOIN_TRIP = "jointrip";
        public static final String LEAVE_TRIP = "leavetrip";
        // endregion

        public enum Function {
            CREATE_NEW_TRIP, UPSERT_USER, UPDATE_TRIP, UPSERT_FCM_TOKEN, JOIN_TRIP, LEAVE_TRIP;
        }

        private String getFunctionName(Enum<Function> function) {
            switch (function.ordinal()) {
                case 0 :
                    return CREATE_NEW_TRIP;
                case 1 :
                    return UPSERT_USER;
                case 2 :
                    return UPDATE_TRIP;
                case 4 :
                    return JOIN_TRIP;
                case 5 :
                    return LEAVE_TRIP;
                default:
                    return UPSERT_FCM_TOKEN;

            }
        }

        public String function;
        public Arguments arguments = new Arguments();

        public Request() { }

        public Request(String function, Arguments arguments) {
            this.function = function;
            this.arguments = arguments;
        }

        public Request(Function function, Arguments arguments) {
            this.function = getFunctionName(function);
            this.arguments = arguments;
        }

        public Request(String function) {
            this.function = function;
        }

        public Request(Function function) {
            this.function = getFunctionName(function);
        }

        public void addArgument(Arguments.Argument argument) {
            this.arguments.add(argument);
        }

        public void addArgument(String argumentValue) {
            Arguments.Argument argument = new Arguments.Argument(null, argumentValue);
            this.arguments.add(argument);
        }

        public String toJson() {
            Gson gson = new Gson();
            return gson.toJson(this);
        }
    }

    public static class Arguments extends ArrayList<Arguments.Argument> {

        public Arguments() { }

        public boolean add(String value) {
            return this.add(new Argument(null, value));
        }

        public boolean add(String name, String value) {
            return this.add(new Argument(name, value));
        }

        @Override
        public String toString() {
            return this.size() + " arguments";
        }

        public static class Argument {
            public String name;
            public Object value;

            public Argument(@Nullable String name, Object value) {
                if (name == null) name = "not supplied";
                this.name = name;
                this.value = value;
            }

            @Override
            public String toString() {
                try {
                    return "Argument: " + name + " | Value: + " + value.toString();
                } catch (Exception e) {
                    return null;
                }
            }
        }
    }

}

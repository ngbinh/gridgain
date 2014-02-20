// @csharp.file.header

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

namespace GridGain.Client.Impl.Message {
    using System;

    /** <summary>Bean representing client operation result.</summary> */
    internal class GridClientResponse {
        /**
         * <summary>
         * Tries to find enum value by operation code.</summary>
         *
         * <param name="val">Operation code value.</param>
         * <returns>Enum value.</returns>
         */
        public static GridClientResponseStatus FindByCode(int val) {
            foreach (GridClientResponseStatus code in Enum.GetValues(typeof(GridClientResponseStatus)))
                if (val == (int)code)
                    return code;

            throw new InvalidOperationException("Invalid status code: " + val);
        }

        /** <summary>Request id.</summary> */
        public long RequestId {
            get;
            set;
        }

        /** <summary>Client id.</summary> */
        public Guid ClientId {
            get;
            set;
        }

        /** <summary>Destination node id.</summary> */
        public Guid DestNodeId {
            get;
            set;
        }

        /** <summary>Client session token.</summary> */
        public byte[] SessionToken {
            get;
            set;
        }
        
        /** <summary>Response status code.</summary> */
        public GridClientResponseStatus Status {
            get;
            set;
        }

        /** <summary>Error message, if any error occurred, or <c>null</c>.</summary> */
        public String ErrorMessage {
            get;
            set;
        }

        /** <summary>Result object.</summary> */
        public Object Result {
            get;
            set;
        }
    }
}